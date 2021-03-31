package com.github.taven.logminer;

import com.github.taven.common.oracle.*;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

public class LogminerStarter {
    public static void main(String[] args) throws IOException {
        InputStream inputStream = LogminerStarter.class.getClassLoader().getResourceAsStream("config.properties");
        Properties config = OracleConfig.load(inputStream);

        String schema = config.getProperty(OracleConfig.jdbcSchema);

        Connection connection = JdbcUtil.createConnection(config.getProperty(OracleConfig.jdbcDriver),
                config.getProperty(OracleConfig.jdbcUrl),
                config.getProperty(OracleConfig.jdbcUser),
                config.getProperty(OracleConfig.jdbcPassword));
        Queue<DatabaseRecord> queue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);


        OracleSnapshotExecutor snapshotExecutor = new OracleSnapshotExecutor(connection, schema, queue);
        SnapshotResult snapshotResult = snapshotExecutor.execute();
        System.out.println("snapshot completed, scn is " + snapshotResult.getScn());
    }
}
