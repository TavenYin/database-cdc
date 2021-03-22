package com.github.taven.oracle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OracleSnapshotExecutor {
    private final Connection connection;
    private final String schema;
    private Map<String, List<TableColumn>> tableStructure;

    public OracleSnapshotExecutor(Connection connection, String schema) {
        this.connection = connection;
        this.schema = schema;
    }

    public SnapshotResult execute() {
        try {
            readTableStructure();

            if (tableStructure == null || tableStructure.isEmpty())
                return SnapshotResult.NULL;

            // read all tables result
            flashbackQuery();

            return null;
        } catch (SQLException e) {
            throw new RuntimeException(e.getMessage(), e);
        }
    }

    private Map<String, List<Object[]>> flashbackQuery() throws SQLException {
        // current scn
        long scn = getCurrentScn();

        try (Statement statement = readTableStatement()) {
            for (String tableName : tableStructure.keySet()) {
                String sql = String.format("SELECT * FROM %s AS OF SCN %s", tableName, scn);
                try (ResultSet rs = statement.executeQuery(sql)) {
                    ResultSetMetaData metaData = rs.getMetaData();

                    while (rs.next()) {
                        Object[] row = new Object[metaData.getColumnCount()];
                        for (int i = 1; i <= metaData.getColumnCount(); i++) {
                            row[i-1] = rs.getObject(i);
                        }

                    }

                }
            }
        }
        return null;
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
