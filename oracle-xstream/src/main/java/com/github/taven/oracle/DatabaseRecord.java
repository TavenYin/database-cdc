package com.github.taven.oracle;

import java.util.List;

public class DatabaseRecord {
    List<TableColumn> columns;
    Object[] row;

    public DatabaseRecord(List<TableColumn> columns, Object[] row) {
        this.columns = columns;
        this.row = row;
    }

    public List<TableColumn> getColumns() {
        return columns;
    }

    public void setColumns(List<TableColumn> columns) {
        this.columns = columns;
    }

    public Object[] getRow() {
        return row;
    }

    public void setRow(Object[] row) {
        this.row = row;
    }
}
