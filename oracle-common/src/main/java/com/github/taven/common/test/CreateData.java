package com.github.taven.common.test;

import com.github.taven.common.util.ConfigUtil;
import com.github.taven.common.util.JdbcUtil;
import com.github.taven.common.oracle.OracleConfig;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.UUID;

public class CreateData {
    public static void main(String[] args) throws IOException, SQLException {
        InputStream inputStream = CreateData.class.getClassLoader().getResourceAsStream("test.properties");
        Properties config = ConfigUtil.load(inputStream);

        String schema = config.getProperty(OracleConfig.jdbcSchema);

        Connection connection = JdbcUtil.createConnection(config.getProperty(OracleConfig.jdbcDriver),
                config.getProperty(OracleConfig.jdbcUrl),
                config.getProperty(OracleConfig.jdbcUser),
                config.getProperty(OracleConfig.jdbcPassword));

        connection.setAutoCommit(false);

        int counter = 10;
        int insertCounter = 100000;

        try (PreparedStatement ps = connection.prepareStatement("insert into SCOTT.TEST_TAB VALUES(?,?,?)")) {
            while (counter > 0) {
                List<String> updateIds = new ArrayList<>();

                for (int i = 0; i < insertCounter; i++) {
                    String uuid = UUID.randomUUID().toString();

                    if (i % 7 == 0) {
                        updateIds.add(uuid);
                    }

                    ps.setString(1, uuid);
                    ps.setBigDecimal(2, new BigDecimal("11.11"));
                    ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    ps.addBatch();
                }

                ps.executeBatch();
                connection.commit();
                System.out.println("insert commit");

                try (Statement statement = connection.createStatement()) {
                    // update
                    String updateSql = String.format("update SCOTT.TEST_TAB set CURRENT_DATE = %s where id in (%s)",
                            new Timestamp(System.currentTimeMillis()), String.join(",", updateIds));
                    statement.execute(updateSql);
                    connection.commit();
                    System.out.println("update commit");

                    // delete
                    statement.execute("delete from SCOTT.TEST_TAB where rownum < 500");
                    connection.commit();
                    System.out.println("delete commit");
                }

                counter--;
            }
        }

    }
}
