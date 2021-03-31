package com.github.taven.common.oracle;

import java.sql.*;
import java.util.*;
import java.util.stream.Collectors;

public class OracleSnapshotExecutor {
    private final Connection connection;
    private final String schema;
    private Map<String, List<TableColumn>> tableStructure;
    private final Queue<DatabaseRecord> queue;

    public OracleSnapshotExecutor(Connection connection, String schema, Queue<DatabaseRecord> queue) {
        this.connection = connection;
        this.schema = schema;
        this.queue = queue;
    }

    public SnapshotResult execute() {
        try {
            readTableStructure();

            if (tableStructure == null || tableStructure.isEmpty())
                return SnapshotResult.NULL;

            // current scn
            long scn = getCurrentScn();

            // read all tables result
            flashbackQuery(scn);

            return new SnapshotResult(tableStructure, true, scn);
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private void flashbackQuery(long scn) throws SQLException {
        try (Statement statement = readTableStatement()) {
            for (Map.Entry<String, List<TableColumn>> entry : tableStructure.entrySet()) {
                String tableName = entry.getKey();
                List<TableColumn> columns = entry.getValue();

                String sql = String.format("SELECT * FROM %s.%s AS OF SCN %s", schema, tableName, scn);
                try (ResultSet rs = statement.executeQuery(sql)) {
                    while (rs.next()) {
                        Object[] row = new Object[columns.size()];
                        for (int i = 0; i < columns.size(); i++) {
                            row[i] = rs.getObject(columns.get(i).getColumnName());
                        }

                        // 如果单纯是线性的读，数据量很大的话会导致OOM，所以正确的做法是一边读一边消费
                        queue.add(new DatabaseRecord(columns, row));
                    }

                }
            }
        }
    }


    private Statement readTableStatement() throws SQLException {
        Statement statement = connection.createStatement();
        statement.setFetchSize(2000);
        return statement;
    }

    private void readTableStructure() throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();

//        try (ResultSet rs = metaData.getTables(null, schema, null, null)) {
//            while (rs.next()) {
//                System.out.println(rs);
//            }
//        }

        List<TableColumn> tableColumnList = new ArrayList<>();

        try (ResultSet rs = metaData.getColumns(null, schema, null, null)) {
            while (rs.next()) {
                TableColumn tableColumn = new TableColumn();
                // 具体索引含义，参考DatabaseMetaData API
                tableColumn.setTableName(rs.getString(3));
                tableColumn.setColumnName(rs.getString(4));
                tableColumn.setNativeType(rs.getString(6));
                tableColumn.setColumnType(JDBCType.valueOf(rs.getInt(5)));
                tableColumnList.add(tableColumn);
            }
        }

        tableStructure = tableColumnList.stream().collect(Collectors.groupingBy(TableColumn::getTableName));
    }


    private long getCurrentScn() throws SQLException {
        try (Statement statement = connection.createStatement()) {
            ResultSet rs = statement.executeQuery("select CURRENT_SCN from V$DATABASE");

            if (!rs.next()) {
                throw new IllegalStateException("Couldn't get SCN");
            }

            return rs.getLong(1);
        }
    }

}
