package com.github.taven.logminer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;

public class LogMinerHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMinerHelper.class);

    public static final int LOG_MINER_OC_INSERT = 1;
    public static final int LOG_MINER_OC_DELETE = 2;
    public static final int LOG_MINER_OC_UPDATE = 3;
    public static final int LOG_MINER_OC_DDL = 5;
    public static final int LOG_MINER_OC_COMMIT = 7;
    public static final int LOG_MINER_OC_MISSING_SCN = 34;
    public static final int LOG_MINER_OC_ROLLBACK = 36;

    public static void removeLogFilesFromMining(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement("SELECT FILENAME AS NAME FROM V$LOGMNR_LOGS");
             ResultSet result = ps.executeQuery()) {
            Set<String> files = new LinkedHashSet<>();
            while (result.next()) {
                files.add(result.getString(1));
            }
            for (String fileName : files) {
                String sql = String.format("BEGIN SYS.DBMS_LOGMNR.REMOVE_LOGFILE(LOGFILENAME => '%s');END;", fileName);
                executeCallableStatement(conn, sql);
                LOGGER.debug("File {} was removed from mining", fileName);
            }
        }
    }

    public static void executeCallableStatement(Connection connection, String statement) throws SQLException {
        Objects.requireNonNull(statement);
        try (CallableStatement s = connection.prepareCall(statement)) {
            s.execute();
        }
    }

    public static List<LogFile> getOnlineLogFilesForOffsetScn(Connection connection, BigInteger offsetScn) throws SQLException {
        List<LogFile> redoLogFiles = new ArrayList<>();

        String onlineLogQuery = "SELECT MIN(F.MEMBER) AS FILE_NAME, L.NEXT_CHANGE# AS NEXT_CHANGE, F.GROUP#, L.FIRST_CHANGE# AS FIRST_CHANGE, L.STATUS " +
                " FROM V$LOG L, V$LOGFILE F " +
                " WHERE F.GROUP# = L.GROUP# AND L.NEXT_CHANGE# > 0 " +
                " GROUP BY F.GROUP#, L.NEXT_CHANGE#, L.FIRST_CHANGE#, L.STATUS ORDER BY 3";

        try (PreparedStatement s = connection.prepareStatement(onlineLogQuery)) {
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    String fileName = rs.getString(1);
                    BigInteger nextChangeNumber = new BigInteger(rs.getString(2));
                    BigInteger firstChangeNumber = new BigInteger(rs.getString(4));
                    String status = rs.getString(5);
                    LogFile logFile = new LogFile(fileName, firstChangeNumber, nextChangeNumber, "CURRENT".equalsIgnoreCase(status));
                    // 添加Current Redo || scn 范围符合的
                    if (logFile.isCurrent() || logFile.getNextScn().compareTo(offsetScn) >= 0) {
                        redoLogFiles.add(logFile);
                    }
                }
            }
        }
        return redoLogFiles;
    }

    public static List<LogFile> getArchivedLogFilesForOffsetScn(Connection connection, BigInteger offsetScn) throws SQLException {
        String archiveLogsQuery = String.format("SELECT NAME AS FILE_NAME, NEXT_CHANGE# AS NEXT_CHANGE, FIRST_CHANGE# AS FIRST_CHANGE FROM V$ARCHIVED_LOG " +
                "WHERE NAME IS NOT NULL AND ARCHIVED = 'YES' " +
                "AND STATUS = 'A' AND NEXT_CHANGE# > %s ORDER BY 2", offsetScn);

        final List<LogFile> archiveLogFiles = new ArrayList<>();
        try (PreparedStatement s = connection.prepareStatement(archiveLogsQuery)) {
            try (ResultSet rs = s.executeQuery()) {
                while (rs.next()) {
                    String fileName = rs.getString(1);
                    BigInteger firstChangeNumber = new BigInteger(rs.getString(3));
                    BigInteger nextChangeNumber = new BigInteger(rs.getString(2));
                    archiveLogFiles.add(new LogFile(fileName, firstChangeNumber, nextChangeNumber, false));
                }
            }
        }
        return archiveLogFiles;
    }

    public static void addLogFile(Connection connection, String fileName) throws SQLException {
        String addLogFile = "BEGIN sys.dbms_logmnr.add_logfile(LOGFILENAME => '%s', OPTIONS => %s);END;";
        String options = "DBMS_LOGMNR.ADDFILE";
        executeCallableStatement(connection, String.format(addLogFile, fileName, options));
    }

    public static List<BigInteger> getCurrentRedoLogSequences(Connection connection) throws SQLException {
        String currentRedoSequence = "SELECT SEQUENCE# FROM V$LOG WHERE STATUS = 'CURRENT'";
        try (Statement statement = connection.createStatement();
             ResultSet rs = statement.executeQuery(currentRedoSequence)) {
            List<BigInteger> sequences = new ArrayList<>();
            if (rs.next()) {
                sequences.add(new BigInteger(rs.getString(1)));
            }
            // 如果是RAC则会返回多个SEQUENCE
            return sequences;
        }
    }

    public static void startLogMiner(Connection connection, BigInteger startScn, BigInteger endScn) throws SQLException {
        // default
        String miningStrategy = "DBMS_LOGMNR.DICT_FROM_ONLINE_CATALOG ";

        String startLogMiner = "BEGIN sys.dbms_logmnr.start_logmnr(" +
                "startScn => '" + startScn + "', " +
                "endScn => '" + endScn + "', " +
                "OPTIONS => " + miningStrategy +
                " + DBMS_LOGMNR.NO_ROWID_IN_STMT);" +
                "END;";

        executeCallableStatement(connection, startLogMiner);
    }

    public static void endLogMiner(Connection connection) {
        try {
            executeCallableStatement(connection, "BEGIN SYS.DBMS_LOGMNR.END_LOGMNR(); END;");
        }
        catch (SQLException e) {
            if (e.getMessage().toUpperCase().contains("ORA-01307")) {
                LOGGER.info("LogMiner session was already closed");
            }
            else {
                LOGGER.error("Cannot close LogMiner session gracefully: {}", e);
            }
        }
    }

    public static String logMinerViewQuery() {
        StringBuilder query = new StringBuilder();
        query.append("SELECT SCN, SQL_REDO, OPERATION_CODE, TIMESTAMP, XID, CSF, TABLE_NAME, SEG_OWNER, OPERATION, USERNAME ");
        query.append("FROM V$LOGMNR_CONTENTS ").append(" ");
        query.append("WHERE ");
        query.append("SCN > ? AND SCN <= ? ");
        query.append("AND (");
        // MISSING_SCN/DDL only when not performed by excluded users
        query.append("(OPERATION_CODE IN (5,34))");
        // COMMIT/ROLLBACK
        query.append("OR (OPERATION_CODE IN (7,36)) ");
        // INSERT/UPDATE/DELETE
        query.append("OR ");
        query.append("(OPERATION_CODE IN (1,2,3) ");
        query.append("))");

        return query.toString();
    }

    public interface ResultSetProcessor {
        void apply(ResultSet rs) throws SQLException;
    }

    public static void executeQuery(PreparedStatement statement, ResultSetProcessor rsProcessor) throws SQLException {
        try (ResultSet rs = statement.executeQuery()) {
            rsProcessor.apply(rs);
        }
    }

}
