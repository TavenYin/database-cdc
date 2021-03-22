package com.github.taven.oracle;

import java.sql.Connection;
import java.sql.DriverManager;

public class JdbcUtil {

    public static Connection createConnection(String driver, String url, String username, String password) {
        try {
            Class.forName(driver);
            return DriverManager.getConnection(url, username, password);
        } catch (Exception e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

}
