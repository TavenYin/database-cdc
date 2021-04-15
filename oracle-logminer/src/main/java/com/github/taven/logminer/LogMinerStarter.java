package com.github.taven.logminer;

import com.github.taven.common.util.ConfigUtil;
import com.github.taven.common.oracle.OracleConfig;
import com.github.taven.common.oracle.*;
import com.github.taven.common.util.JdbcUtil;
import com.github.taven.logminer.consumer.MySQLSink;
import com.github.taven.logminer.consumer.LogMinerSink;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigInteger;
import java.sql.Connection;
import java.util.Properties;

public class LogMinerStarter {
    private static final Logger logger = LoggerFactory.getLogger(LogMinerStarter.class);

    public static void main(String[] args) throws IOException {
        InputStream inputStream = LogMinerStarter.class.getClassLoader().getResourceAsStream("oracle_config.properties");
        Properties config = ConfigUtil.load(inputStream);

        String schema = config.getProperty(OracleConfig.jdbcSchema);

        Connection connection = JdbcUtil.createConnection(config.getProperty(OracleConfig.jdbcDriver),
                config.getProperty(OracleConfig.jdbcUrl),
                config.getProperty(OracleConfig.jdbcUser),
                config.getProperty(OracleConfig.jdbcPassword));

        LogMinerSink mysqlSink = new MySQLSink();
        mysqlSink.init();

        logger.debug("start snapshot...");
        OracleSnapshotExecutor snapshotExecutor = new OracleSnapshotExecutor(connection, schema, mysqlSink::handleSnapshot);
        SnapshotResult snapshotResult = snapshotExecutor.execute();
        logger.debug("snapshot completed, scn is " + snapshotResult.getScn());

        LogMinerCDC logMinerCDC = new LogMinerCDC(connection,
                BigInteger.valueOf(snapshotResult.getScn()), mysqlSink, config);
        logMinerCDC.start();
    }
}
