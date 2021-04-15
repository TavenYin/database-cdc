package com.github.taven.logminer.consumer;

import com.github.taven.common.util.ConfigUtil;
import com.github.taven.common.util.JdbcUtil;
import com.github.taven.common.oracle.OracleConfig;
import com.github.taven.common.oracle.TableColumn;
import com.github.taven.logminer.LogMinerDmlObject;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

public class MySQLSink implements LogMinerSink {
    private Connection connection;

    public MySQLSink() {
    }

    public void init() {
        try {
            InputStream inputStream = MySQLSink.class.getClassLoader().getResourceAsStream("mysql_config.properties");
            Properties config = ConfigUtil.load(inputStream);
            this.connection = JdbcUtil.createConnection(config.getProperty(MySQLConfig.jdbcDriver),
                    config.getProperty(MySQLConfig.jdbcUrl),
                    config.getProperty(MySQLConfig.jdbcUser),
                    config.getProperty(MySQLConfig.jdbcPassword));

            connection.setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleSnapshot(List<TableColumn> tableColumns, List<Object[]> records) {
        StringBuilder insertSQL = new StringBuilder("INSERT INTO ")
                .append(tableColumns.get(0).getTableName())
                .append(" ( ");
        List<String> values = new ArrayList<>();

        tableColumns.forEach(tableColumn -> {
            insertSQL.append(tableColumn.getColumnName())
                    .append(" ");
            values.add("?");
        });

        insertSQL.append(" ) VALUES (")
                .append(String.join(",", values))
                .append(")");

        try (PreparedStatement ps = connection.prepareStatement(insertSQL.toString())) {
            for (Object[] row : records) {
                for (int i = 0, rowLength = row.length; i < rowLength; i++) {
                    Object columnValue = row[i];
                    ps.setObject(i + 1, columnValue);
                }
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void handleLogMinerDml(LogMinerDmlObject dmlObject) {

    }
}
