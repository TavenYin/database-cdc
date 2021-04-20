package com.github.taven.logminer.consumer;

public interface MySQLConfig {
    String jdbcDriver = "mysql.jdbc.driver";
    String jdbcUrl = "mysql.jdbc.url";
    String jdbcUser = "mysql.jdbc.user";
    String jdbcPassword = "mysql.jdbc.password";

    String consumerEnabled = "consumer.enabled";
}
