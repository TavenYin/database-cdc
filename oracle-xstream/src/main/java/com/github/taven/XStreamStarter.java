package com.github.taven;

import com.github.taven.oracle.JdbcUtil;
import com.github.taven.oracle.OracleConfig;
import com.github.taven.oracle.OracleSnapshotExecutor;

import java.io.IOException;
import java.io.InputStream;
import java.sql.Connection;
import java.util.Properties;

/**
 * 通过闪回 + XStream 实现Oracle 全量 + 增量数据迁移
 * 注：未考虑表结构变动的情况
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

        OracleSnapshotExecutor snapshotExecutor = new OracleSnapshotExecutor(connection, schema);
        snapshotExecutor.execute();

    }

}
