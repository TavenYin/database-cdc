package com.github.taven.test;

import com.github.taven.XStreamStarter;
import com.github.taven.oracle.JdbcUtil;
import com.github.taven.oracle.OracleConfig;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.sql.*;
import java.util.Properties;
import java.util.UUID;

public class CreateData {
    public static void main(String[] args) throws IOException, SQLException {
        InputStream inputStream = XStreamStarter.class.getClassLoader().getResourceAsStream("test.properties");
        Properties config = OracleConfig.load(inputStream);

        String schema = config.getProperty(OracleConfig.jdbcSchema);

        Connection connection = JdbcUtil.createConnection(config.getProperty(OracleConfig.jdbcDriver),
                config.getProperty(OracleConfig.jdbcUrl),
                config.getProperty(OracleConfig.jdbcUser),
                config.getProperty(OracleConfig.jdbcPassword));

        while (true) {
            try (PreparedStatement ps = connection.prepareStatement("insert into SCOTT.TEST_TAB VALUES(?,?,?)")) {
                connection.setAutoCommit(false);

                for (int i = 0; i < 10000 ; i++) {
                    ps.setString(1, UUID.randomUUID().toString());
                    ps.setBigDecimal(2, new BigDecimal("11.11"));
                    ps.setTimestamp(3, new Timestamp(System.currentTimeMillis()));
                    ps.addBatch();
                }

                ps.executeBatch();
                connection.commit();
                System.out.println("insert commit");

                // update
//                Statement statement = connection.createStatement();
//                statement.execute("update SCOTT.TEST_TAB set price = 22.22 where rownum < 1000");
//                connection.commit();
//                System.out.println("update commit");

//                // delete
//                statement.execute("delete from SCOTT.TEST_TAB where rownum < 500");
//                connection.commit();
//                System.out.println("update commit");
            }
        }

    }
}
