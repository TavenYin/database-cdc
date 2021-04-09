package com.github.taven.logminer;

import java.math.BigInteger;
import java.util.Objects;

public class LogFile {
    private final String fileName;
    private final BigInteger firstScn;
    private final BigInteger nextScn;
    private final boolean current;

    public LogFile(String fileName, BigInteger firstScn, BigInteger nextScn, boolean current) {
        this.fileName = fileName;
        this.firstScn = firstScn;
        this.nextScn = nextScn;
        this.current = current;
    }

    public String getFileName() {
        return fileName;
    }

    public BigInteger getFirstScn() {
        return firstScn;
    }

    public BigInteger getNextScn() {
        return nextScn;
    }

    public boolean isCurrent() {
        return current;
    }

    public boolean isSameRange(LogFile other) {
        return Objects.equals(firstScn, other.getFirstScn()) && Objects.equals(nextScn, other.getNextScn());
    }
}
