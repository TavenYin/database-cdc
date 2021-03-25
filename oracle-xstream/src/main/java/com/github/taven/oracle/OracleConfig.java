package com.github.taven.oracle;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

public class OracleConfig {
    public static final String jdbcDriver = "oracle.jdbc.driver";
    public static final String jdbcUrl = "oracle.jdbc.url";
    public static final String jdbcUser = "oracle.jdbc.user";
    public static final String jdbcPassword = "oracle.jdbc.password";
    public static final String jdbcSchema = "oracle.jdbc.schema";
    public static final String ociUrl = "oracle.xstream.url";
    public static final String outboundServer = "oracle.xstream.outboundServer";

    private OracleConfig() {}

    public static Properties load(InputStream inputStream) throws IOException {
        Properties properties = new Properties();
        properties.load(inputStream);
        return properties;
    }

}
