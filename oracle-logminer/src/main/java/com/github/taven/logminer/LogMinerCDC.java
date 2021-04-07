package com.github.taven.logminer;

import com.github.taven.common.oracle.DatabaseRecord;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.Queue;

public class LogMinerCDC {
    private Connection oracleConnection;
    private long startScn;
    private Queue<DatabaseRecord> queue;

    public void start() {
        try {
            // 记录当前redoLog，用于下文判断redoLog 是否切换

            // 构建数据字典 && add redo / archived log
            initializeLogMiner();

            // while

            // 确定 endScn

            // 是否发生redoLog切换
            // 如果切换则重启logMiner流程

            // start logMiner

            // 查询 logMiner view

            // 确定新的SCN
        } catch (SQLException e) {
            e.printStackTrace();
        }

    }


    private void initializeLogMiner() throws SQLException {
        // 默认使用在线数据字典，所以此处不做数据字典相关操作

        setRedoLog();
    }

    private void setRedoLog() throws SQLException {

        LogMinerHelper.removeLogFilesFromMining(oracleConnection);


    }
}
