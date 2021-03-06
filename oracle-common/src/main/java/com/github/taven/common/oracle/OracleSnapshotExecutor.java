package com.github.taven.common.oracle;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OracleSnapshotExecutor {
    private final Connection connection;
    private final String schema;
    private Map<String, List<TableColumn>> tableStructure;
    private final SnapshotCallback callback;

    public OracleSnapshotExecutor(Connection connection, String schema, SnapshotCallback callback) {
        this.connection = connection;
        this.schema = schema;
        this.callback = callback;
    }

    public SnapshotResult execute() {
        try {
            readTableStructure();

            if (tableStructure == null || tableStructure.isEmpty())
                return SnapshotResult.NULL;

            // current scn
            long scn = OracleHelper.getCurrentScn(connection);

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
                List<Object[]> records = new ArrayList<>();

                try (ResultSet rs = statement.executeQuery(sql)) {
                    while (rs.next()) {
                        Object[] row = new Object[columns.size()];
                        for (int i = 0; i < columns.size(); i++) {
                            row[i] = rs.getObject(columns.get(i).getColumnName());
                        }

                        records.add(row);

                        // ????????????????????????????????????????????????????????????OOM????????????????????????????????????????????????
                        pushIfNecessary(columns, records, false);
                    }

                }

                pushIfNecessary(columns, records, true);
            }
        }

    }



    private void pushIfNecessary(List<TableColumn> columns, List<Object[]> records, boolean force) {
        if (records.size() > 5000 || force) {
            callback.apply(columns, records);
            records.clear();
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
                // ???????????????????????????DatabaseMetaData API
                tableColumn.setTableName(rs.getString(3));
                tableColumn.setColumnName(rs.getString(4));
                tableColumn.setNativeType(rs.getString(6));
                tableColumn.setColumnType(JDBCType.valueOf(rs.getInt(5)));
                tableColumnList.add(tableColumn);
            }
        }

        tableStructure = tableColumnList.stream().collect(Collectors.groupingBy(TableColumn::getTableName));
    }

}
