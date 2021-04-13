package com.github.taven.logminer;

import com.github.taven.common.oracle.DatabaseRecord;
import com.github.taven.common.oracle.OracleHelper;

import java.math.BigInteger;
import java.sql.*;
import java.util.List;
import java.util.Queue;
import java.util.stream.Collectors;

public class LogMinerCDC {
    private Connection connection;
    private BigInteger startScn;
    private Queue<DatabaseRecord> queue;
    private List<BigInteger> currentRedoLogSequences;

    public LogMinerCDC(Connection connection, BigInteger startScn, Queue<DatabaseRecord> queue) {
        this.connection = connection;
        this.startScn = startScn;
        this.queue = queue;
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
                // 将TransactionalBuffer中当前事务的DML 转移到消费者处理
                // continue

            // Rollback
                // 清空TransactionalBuffer中当前事务
                // continue

            // DDL
                // continue

            // MISSING_SCN
                // continue

            // DML
                // 内部维护 TransactionalBuffer，将每条DML注册到Buffer中
                // 根据事务提交或者回滚情况决定如何处理

        }
    }

    private String getRedoSQL(ResultSet rs) {
        return null;
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
