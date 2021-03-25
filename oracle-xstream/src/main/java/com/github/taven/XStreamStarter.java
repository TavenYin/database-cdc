package com.github.taven;

import com.github.taven.oracle.*;
import com.github.taven.xstream.OracleXStreamCDC;
import oracle.jdbc.OracleConnection;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;
import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * 通过闪回 + XStream 实现Oracle 全量 + 增量数据迁移
 * 注：未考虑表结构变动的情况
 *
 * -Djava.library.path=/instantclient_12_1
 *
 */
public class XStreamStarter {

    public static void main(String[] args) throws IOException {
        InputStream inputStream = XStreamStarter.class.getClassLoader().getResourceAsStream("config.properties");
        Properties config = OracleConfig.load(inputStream);

        String schema = config.getProperty(OracleConfig.jdbcSchema);

        Connection connection = JdbcUtil.createConnection(config.getProperty(OracleConfig.jdbcDriver),
                config.getProperty(OracleConfig.jdbcUrl),
                config.getProperty(OracleConfig.jdbcUser),
                config.getProperty(OracleConfig.jdbcPassword));
        Queue<DatabaseRecord> queue = new LinkedBlockingQueue<>(Integer.MAX_VALUE);


        OracleSnapshotExecutor snapshotExecutor = new OracleSnapshotExecutor(connection, schema, queue);
        SnapshotResult snapshotResult = snapshotExecutor.execute();
        System.out.println(snapshotResult);

        // 启动增量
        Connection ociConnection = JdbcUtil.createConnection(config.getProperty(OracleConfig.jdbcDriver),
                config.getProperty(OracleConfig.ociUrl),
                config.getProperty(OracleConfig.jdbcUser),
                config.getProperty(OracleConfig.jdbcPassword));
        OracleXStreamCDC xStreamCDC = new OracleXStreamCDC();
        xStreamCDC.start((OracleConnection) ociConnection,
                config.getProperty(OracleConfig.outboundServer), snapshotResult.getScn());
    }

}
