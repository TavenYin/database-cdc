package com.github.taven.common.oracle;

import java.sql.JDBCType;

public class TableColumn {
    private String tableName;
    private String columnName;
    private String nativeType;
    private JDBCType columnType;

    public String getTableName() {
        return tableName;
    }

    public void setTableName(String tableName) {
        this.tableName = tableName;
    }

    public String getColumnName() {
        return columnName;
    }

    public void setColumnName(String columnName) {
        this.columnName = columnName;
    }

    public String getNativeType() {
        return nativeType;
    }

    public void setNativeType(String nativeType) {
        this.nativeType = nativeType;
    }

    public JDBCType getColumnType() {
        return columnType;
    }

    public void setColumnType(JDBCType columnType) {
        this.columnType = columnType;
    }
}
