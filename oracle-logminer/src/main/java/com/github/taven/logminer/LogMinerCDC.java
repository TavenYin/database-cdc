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
                        // 如果切换则重启logMiner流程
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
                }

            }

        } catch (SQLException e) {
            e.printStackTrace();
        }

    }

    private void logMinerViewProcessor(ResultSet rs) {

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