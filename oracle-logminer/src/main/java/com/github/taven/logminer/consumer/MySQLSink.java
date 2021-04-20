package com.github.taven.logminer.consumer;

import com.alibaba.druid.DbType;
import com.alibaba.druid.sql.SQLUtils;
import com.alibaba.druid.sql.ast.SQLExpr;
import com.alibaba.druid.sql.ast.SQLStatement;
import com.alibaba.druid.sql.ast.expr.SQLIdentifierExpr;
import com.alibaba.druid.sql.ast.expr.SQLMethodInvokeExpr;
import com.alibaba.druid.sql.ast.statement.SQLInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleDeleteStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleInsertStatement;
import com.alibaba.druid.sql.dialect.oracle.ast.stmt.OracleUpdateStatement;
import com.github.taven.common.oracle.TableColumn;
import com.github.taven.common.util.ConfigUtil;
import com.github.taven.common.util.JdbcUtil;
import com.github.taven.logminer.LogMinerDmlObject;
import oracle.sql.TIMESTAMP;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.JDBCType;
import java.sql.PreparedStatement;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class MySQLSink implements LogMinerSink {
    private Connection connection;
    private static final Logger logger = LoggerFactory.getLogger(MySQLSink.class);

    public MySQLSink() {
    }

    public void init() {
        try {
            InputStream inputStream = MySQLSink.class.getClassLoader().getResourceAsStream("mysql_config.properties");
            Properties config = ConfigUtil.load(inputStream);
            this.connection = JdbcUtil.createConnection(config.getProperty(MySQLConfig.jdbcDriver),
                    config.getProperty(MySQLConfig.jdbcUrl),
                    config.getProperty(MySQLConfig.jdbcUser),
                    config.getProperty(MySQLConfig.jdbcPassword));

            connection.setAutoCommit(false);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public void handleSnapshot(List<TableColumn> tableColumns, List<Object[]> records) {
        StringBuilder insertSQL = new StringBuilder("INSERT INTO ")
                .append(tableColumns.get(0).getTableName())
                .append(" ( ");
        List<String> columns = new ArrayList<>();
        List<String> values = new ArrayList<>();

        tableColumns.forEach(tableColumn -> {
            columns.add(tableColumn.getColumnName());
            values.add("?");
        });


        insertSQL.append(String.join(",", columns))
                .append(" ) VALUES (")
                .append(String.join(",", values))
                .append(")");

        try (PreparedStatement ps = connection.prepareStatement(insertSQL.toString())) {
            for (Object[] row : records) {
                for (int i = 0, rowLength = row.length; i < rowLength; i++) {
                    Object columnValue = row[i];
                    if (tableColumns.get(i).getColumnType().equals(JDBCType.TIMESTAMP)) {
                        ps.setTimestamp(i + 1,
                                columnValue != null ? ((TIMESTAMP) columnValue).timestampValue() : null);
                    } else {
                        ps.setObject(i + 1, columnValue);
                    }
                }
                ps.addBatch();
            }

            ps.executeBatch();
            connection.commit();
            logger.debug("snapshot commit");

        } catch (Exception e) {
            throw new RuntimeException(e);
        }

    }

    public void handleLogMinerDml(LogMinerDmlObject dmlObject) {
        // 我写这个消费者的目的就是为了确认程序是否能捕获到Oracle的所有更改
        // 使用Druid自带的工具类进行SQL解析，这里写的有点乱 不要在意
        List<SQLStatement> sqlStatements = SQLUtils.parseStatements(dmlObject.getRedoSql(), DbType.oracle);
        for (SQLStatement sqlStatement : sqlStatements) {
            if (sqlStatement instanceof OracleInsertStatement) {
                OracleInsertStatement oracleStatement = (OracleInsertStatement) sqlStatement;
                String tableName = oracleStatement.getTableName().getSimpleName().replace("\"", "");
                List<String> columns = oracleStatement.getColumns()
                        .stream().map(c -> ((SQLIdentifierExpr) c).getName().replace("\"", "")).collect(Collectors.toList());
                List<SQLInsertStatement.ValuesClause> valuesList = oracleStatement.getValuesList();
                List<String> values = valuesList.get(0).getValues()
                        .stream().map(sqlExpr -> {
                            if (sqlExpr instanceof SQLMethodInvokeExpr) {
                                return handleOracleFunction(sqlExpr);
                            } else {
                                return sqlExpr.toString();
                            }
                        }).collect(Collectors.toList());
                StringBuilder insertBuilder = new StringBuilder();
                insertBuilder.append("INSERT INTO ")
                        .append(tableName)
                        .append(" ")
                        .append("(")
                        .append(String.join(",", columns))
                        .append(")")
                        .append(" VALUES ")
                        .append("(")
                        .append(String.join(",", values))
                        .append(")");
                try (Statement statement = connection.createStatement()) {
                    statement.execute(insertBuilder.toString());
                    connection.commit();
                } catch (Exception e) {
                    logger.error(e.getMessage(), e);
                }

            } else if (sqlStatement instanceof OracleUpdateStatement) {

            } else if (sqlStatement instanceof OracleDeleteStatement) {

            } else {
                throw new RuntimeException("not supported SQLStatement");
            }
        }
    }

    private String handleOracleFunction(SQLExpr sqlExpr) {
        SQLMethodInvokeExpr sqlFunction = (SQLMethodInvokeExpr) sqlExpr;
        String methodName = sqlFunction.getMethodName();
        String value;
        switch (methodName) {
            case ("TO_TIMESTAMP"): {
                value = sqlFunction.getArguments().get(0).toString();
                break;
            }
            default:
                throw new RuntimeException("not supported function");
        }

        return value;
    }

}
