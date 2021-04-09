package com.github.taven.logminer;

import oracle.jdbc.OracleConnection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.math.BigInteger;
import java.sql.*;
import java.util.*;

public class LogMinerHelper {
    private static final Logger LOGGER = LoggerFactory.getLogger(LogMinerHelper.class);

    static final String FILES_FOR_MINING = "SELECT FILENAME AS NAME FROM V$LOGMNR_LOGS";
    static final String DELETE_LOGFILE_TEMPLATE = "BEGIN SYS.DBMS_LOGMNR.REMOVE_LOGFILE(LOGFILENAME => '%s');END;";
    static final String ONLINE_LOGS_QUERY = "SELECT MIN(F.MEMBER) AS FILE_NAME, L.NEXT_CHANGE# AS NEXT_CHANGE, F.GROUP#, L.FIRST_CHANGE# AS FIRST_CHANGE, L.STATUS " +
            " FROM V$LOG L, V$LOGFILE F " +
            " WHERE F.GROUP# = L.GROUP# AND L.NEXT_CHANGE# > 0 " +
            " GROUP BY F.GROUP#, L.NEXT_CHANGE#, L.FIRST_CHANGE#, L.STATUS ORDER BY 3";

    public static void removeLogFilesFromMining(Connection conn) throws SQLException {
        try (PreparedStatement ps = conn.prepareStatement(FILES_FOR_MINING);
             ResultSet result = ps.executeQuery()) {
            Set<String> files = new LinkedHashSet<>();
            while (result.next()) {
                files.add(result.getString(1));
            }
            for (String fileName : files) {
                executeCallableStatement(conn, String.format(DELETE_LOGFILE_TEMPLATE, fileName));
                LOGGER.debug("File {} was removed from mining", fileName);
            }
        }
    }

    private static void executeCallableStatement(Connection connection, String statement) throws SQLException {
        Objects.requireNonNull(statement);
        try (CallableStatement s = connection.prepareCall(statement)) {
            s.execute();
        }
    }

    public static List<LogFile> getOnlineLogFilesForOffsetScn(Connection connection, BigInteger offsetScn) throws SQLException {
        List<LogFile> redoLogFiles = new ArrayList<>();

        try (PreparedStatement s = connection.prepareStatement(ONLINE_LOGS_QUERY)) {
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

    public static List<LogFile> getArchivedLogFilesForOffsetScn(OracleConnection connection, BigInteger offsetScn) throws SQLException {
        final List<LogFile> archiveLogFiles = new ArrayList<>();
        try (PreparedStatement s = connection.prepareStatement(SqlUtils.archiveLogsQuery(offsetScn))) {
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

}
