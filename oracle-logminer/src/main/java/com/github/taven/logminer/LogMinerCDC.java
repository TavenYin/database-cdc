package com.github.taven.logminer;

import com.github.taven.common.consumer.ConsumerThreadPool;
import com.github.taven.common.oracle.OracleHelper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.*;
import java.util.List;
import java.util.stream.Collectors;

public class LogMinerCDC {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMinerCDC.class);
    private final Connection connection;
    private BigInteger startScn;
    private final ConsumerThreadPool consumerThreadPool;
    private List<BigInteger> currentRedoLogSequences;
    private final TransactionalBuffer transactionalBuffer;
    private OffsetContext offsetContext;

    public LogMinerCDC(Connection connection, BigInteger startScn, ConsumerThreadPool consumerThreadPool) {
        this.connection = connection;
        this.startScn = startScn;
        this.consumerThreadPool = consumerThreadPool;
        this.transactionalBuffer = new TransactionalBuffer();
    }

    public void start() {
        try {
            // 1.记录当前redoLog，用于下文判断redoLog 是否切换
            currentRedoLogSequences = LogMinerHelper.getCurrentRedoLogSequences(connection);

            // 2.构建数据字典 && add redo / archived log
            initializeLogMiner();

            String minerViewQuery = LogMinerHelper.logMinerViewQuery();
            try (PreparedStatement minerViewStatement = connection.prepareStatement(minerViewQuery, ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY, ResultSet.HOLD_CURSORS_OVER_COMMIT)) {
                // while
                while (true) {
                    // 3.确定 endScn
                    BigInteger endScn = determineEndScn();

                    // 4.是否发生redoLog切换
                    if (redoLogSwitchOccurred()) {
                        // 如果切换则重启logMiner会话
                        restartLogMiner();
                    }

                    // 5.start logMiner
                    LogMinerHelper.startLogMiner(connection, startScn, endScn);

                    // 6.查询 logMiner view, 处理结果集
                    minerViewStatement.setFetchSize(2000);
                    minerViewStatement.setFetchDirection(ResultSet.FETCH_FORWARD);
                    minerViewStatement.setString(1, startScn.toString());
                    minerViewStatement.setString(2, endScn.toString());
                    LogMinerHelper.executeQuery(minerViewStatement, this::logMinerViewProcessor);

                    // 7.确定新的SCN
                    startScn = endScn;

                    // 8.重启后的StartScn
                    if (transactionalBuffer.isEmpty()) {
                        offsetContext.offsetScn = startScn.longValue();
                    }
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void logMinerViewProcessor(ResultSet rs) throws SQLException {
        while (rs.next()) {
            BigInteger scn = rs.getBigDecimal("SCN").toBigInteger();
            String tableName = rs.getString("TABLE_NAME");
            String segOwner = rs.getString("SEG_OWNER");
            int operationCode = rs.getInt("OPERATION_CODE");
            Timestamp changeTime = rs.getTimestamp("TIMESTAMP");
            String txId = rs.getString("XID");
            String operation = rs.getString("OPERATION");
            String username = rs.getString("USERNAME");

            String redoSql = getRedoSQL(rs);

            // Commit
            if (operationCode == LogMinerHelper.LOG_MINER_OC_COMMIT) {
                // 将TransactionalBuffer中当前事务的DML 转移到消费者处理
                if (transactionalBuffer.commit(txId, scn, offsetContext)) {
                    LOGGER.debug("txId: {} commit", txId);
                }
                continue;
            }

            // Rollback
            if (operationCode == LogMinerHelper.LOG_MINER_OC_ROLLBACK) {
                // 清空TransactionalBuffer中当前事务
                if (transactionalBuffer.rollback(txId)) {
                    LOGGER.debug("txId: {} rollback", txId);
                }
                continue;
            }

            // DDL
            if (operationCode == LogMinerHelper.LOG_MINER_OC_DDL) {
                LOGGER.info("DDL: {}", redoSql);
                continue;
            }

            // MISSING_SCN
            // MISSING_SCN
            if (operationCode == LogMinerHelper.LOG_MINER_OC_MISSING_SCN) {
                LOGGER.warn("Found MISSING_SCN");
                continue;
            }

            // DML
            if (operationCode == LogMinerHelper.LOG_MINER_OC_INSERT
                    || operationCode == LogMinerHelper.LOG_MINER_OC_DELETE
                    || operationCode == LogMinerHelper.LOG_MINER_OC_UPDATE) {
                // 内部维护 TransactionalBuffer，将每条DML注册到Buffer中
                // 根据事务提交或者回滚情况决定如何处理
                if (redoSql != null) {
                    LOGGER.debug("txId: {}, dml: {}", txId, redoSql);
                    LogMinerDmlObject dmlObject = new LogMinerDmlObject(redoSql, segOwner, tableName, changeTime, txId, scn);
                    // Transactional Commit Callback
                    TransactionalBuffer.CommitCallback commitCallback = (smallestScn, commitScn, counter) -> {
                        if (smallestScn == null || scn.compareTo(smallestScn) < 0) {
                            // 当前SCN 事务已经提交 并且 小于事务缓冲区中所有的开始SCN，所以可以更新offsetScn
                            offsetContext.offsetScn = scn.longValue();
                        }

                        if (counter == 0) {
                            offsetContext.setCommittedScn(commitScn.longValue());
                        }

                        // 消费LogMiner数据
                        consumerThreadPool.asyncConsume(() -> LogMinerDmlConsumer.consume(dmlObject));
                    };

                    transactionalBuffer.registerCommitCallback(txId, scn, changeTime.toInstant(), commitCallback);
                }

            }

        }
    }

    private String getRedoSQL(ResultSet rs) throws SQLException {
        String redoSql = rs.getString("SQL_REDO");
        if (redoSql == null) {
            return null;
        }
        StringBuilder redoBuilder = new StringBuilder(redoSql);

        // https://docs.oracle.com/cd/B19306_01/server.102/b14237/dynviews_1154.htm#REFRN30132
        // Continuation SQL flag. Possible values are:
        // 0 = indicates SQL_REDO and SQL_UNDO is contained within the same row
        // 1 = indicates that either SQL_REDO or SQL_UNDO is greater than 4000 bytes in size and is continued in the next row returned by the view
        int csf = rs.getInt("CSF");

        while (csf == 1) {
            rs.next();
            redoBuilder.append(rs.getString("SQL_REDO"));
            csf = rs.getInt("CSF");
        }

        return redoBuilder.toString();
    }

    private void restartLogMiner() throws SQLException {
        LogMinerHelper.endLogMiner(connection);
        initializeLogMiner();
    }

    private void initializeLogMiner() throws SQLException {
        // 默认使用在线数据字典，所以此处不做数据字典相关操作

        setRedoLog();
    }

    private void setRedoLog() throws SQLException {
        LogMinerHelper.removeLogFilesFromMining(connection);

        List<LogFile> archivedLogFiles = LogMinerHelper.getArchivedLogFilesForOffsetScn(connection, startScn);
        List<LogFile> onlineLogFiles = LogMinerHelper.getOnlineLogFilesForOffsetScn(connection, startScn);

        List<String> logFilesNames = archivedLogFiles.stream().map(LogFile::getFileName).collect(Collectors.toList());

        for (LogFile onlineLogFile : onlineLogFiles) {
            for (LogFile archivedLogFile : archivedLogFiles) {
                if (onlineLogFile.isSameRange(archivedLogFile)) {
                    // 如果redo 已经被归档，那么就不需要加载这个redo了
                    break;
                } else {
                    logFilesNames.add(onlineLogFile.getFileName());
                }
            }
        }

        // 加载所需要的redo / archived
        for (String fileName : logFilesNames) {
            LogMinerHelper.addLogFile(connection, fileName);
        }
    }

    private BigInteger determineEndScn() throws SQLException {
        return BigInteger.valueOf(OracleHelper.getCurrentScn(connection));
    }

    private boolean redoLogSwitchOccurred() throws SQLException {
        final List<BigInteger> newSequences = LogMinerHelper.getCurrentRedoLogSequences(connection);
        if (!newSequences.equals(currentRedoLogSequences)) {
            currentRedoLogSequences = newSequences;
            return true;
        }
        return false;
    }

}
