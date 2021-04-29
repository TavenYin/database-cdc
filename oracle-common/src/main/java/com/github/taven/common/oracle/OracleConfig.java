package com.github.taven.common.oracle;


public interface OracleConfig {
    String jdbcDriver = "oracle.jdbc.driver";
    String jdbcUrl = "oracle.jdbc.url";
    String jdbcUser = "oracle.jdbc.user";
    String jdbcPassword = "oracle.jdbc.password";
    String jdbcSchema = "oracle.jdbc.schema";
    String ociUrl = "oracle.xstream.url";
    String outboundServer = "oracle.xstream.outboundServer";
    String miningStrategy = "oracle.mining.strategy";
    String oracleVersion = "oracle.version";
    String miningSinkClass = "oracle.mining.sinkClass";
    String miningDefaultBatch = "oracle.mining.defaultBatch";
    String miningMinBatch = "oracle.mining.minBatch";
    String miningMaxBatch = "oracle.mining.maxBatch";
    String miningSleepTime = "oracle.mining.sleepTime";
    String miningMinSleepTime = "oracle.mining.minSleepTime";
    String miningMaxSleepTime = "oracle.mining.maxSleepTime";
}
