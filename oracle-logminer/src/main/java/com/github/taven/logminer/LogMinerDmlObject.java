package com.github.taven.logminer;

import java.math.BigInteger;
import java.sql.Timestamp;

public class LogMinerDmlObject {
    private String redoSql;
    private String objectOwner;
    private String objectName;
    private Timestamp sourceTime;
    private String transactionId;
    private BigInteger scn;

    public LogMinerDmlObject() {
    }

    public LogMinerDmlObject(String redoSql, String objectOwner, String objectName, Timestamp sourceTime, String transactionId, BigInteger scn) {
        this.redoSql = redoSql;
        this.objectOwner = objectOwner;
        this.objectName = objectName;
        this.sourceTime = sourceTime;
        this.transactionId = transactionId;
        this.scn = scn;
    }

    public String getRedoSql() {
        return redoSql;
    }

    public void setRedoSql(String redoSql) {
        this.redoSql = redoSql;
    }

    public String getObjectOwner() {
        return objectOwner;
    }

    public void setObjectOwner(String objectOwner) {
        this.objectOwner = objectOwner;
    }

    public String getObjectName() {
        return objectName;
    }

    public void setObjectName(String objectName) {
        this.objectName = objectName;
    }

    public Timestamp getSourceTime() {
        return sourceTime;
    }

    public void setSourceTime(Timestamp sourceTime) {
        this.sourceTime = sourceTime;
    }

    public String getTransactionId() {
        return transactionId;
    }

    public void setTransactionId(String transactionId) {
        this.transactionId = transactionId;
    }

    public BigInteger getScn() {
        return scn;
    }

    public void setScn(BigInteger scn) {
        this.scn = scn;
    }

    @Override
    public String toString() {
        return "LogMinerDmlObject{" +
                "redoSql='" + redoSql + '\'' +
                ", objectOwner='" + objectOwner + '\'' +
                ", objectName='" + objectName + '\'' +
                ", sourceTime=" + sourceTime +
                ", transactionId='" + transactionId + '\'' +
                ", scn=" + scn +
                '}';
    }
}
