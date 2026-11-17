package com.ottawa311.db;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Creates the tables and demo accounts from schema.sql when they are missing, so the WAR
 * works on a machine that only has an empty MySQL server. schema.sql is idempotent
 * (CREATE TABLE IF NOT EXISTS, INSERT IGNORE), so running it again never deletes data.
 * Uses DatabaseMetaData (JDBC metadata) to find out whether the tables already exist.
 */
public final class SchemaInitializer {

    private static final String[] REQUIRED_TABLES = {"users", "reports"};

    private SchemaInitializer() {
    }

    /** @return true if the schema script was run, false if the tables were already there. */
    public static boolean ensureSchema() throws SQLException, IOException {
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            if (allTablesExist(conn)) {
                return false;
            }
            List<String> statements = splitStatements(readSchema());
            try (Statement stmt = conn.createStatement()) {
                for (String sql : statements) {
                    String head = sql.toUpperCase(Locale.ROOT);
                    // The connection already points at the database, so skip these two.
                    if (head.startsWith("CREATE DATABASE") || head.startsWith("USE ")) {
                        continue;
                    }
                    stmt.execute(sql);
                }
            }
            return true;
        }
    }

    private static boolean allTablesExist(Connection conn) throws SQLException {
        DatabaseMetaData meta = conn.getMetaData();
        for (String table : REQUIRED_TABLES) {
            try (ResultSet rs = meta.getTables(conn.getCatalog(), null, table, new String[] {"TABLE"})) {
                if (!rs.next()) {
                    return false;
                }
            }
        }
        return true;
    }

    private static String readSchema() throws IOException {
        try (InputStream in = SchemaInitializer.class.getClassLoader().getResourceAsStream("schema.sql")) {
            if (in == null) {
                throw new IOException("schema.sql was not found on the classpath");
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            return new String(out.toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /** Remove "-- ..." comments and split the script on semicolons that are outside quotes. */
   public static List<String> splitStatements(String script) {
        StringBuilder clean = new StringBuilder();
        boolean inQuote = false;
        for (int i = 0; i < script.length(); i++) {
            char c = script.charAt(i);
            if (c == '\'') {
                inQuote = !inQuote;
            }
            if (!inQuote && c == '-' && i + 1 < script.length() && script.charAt(i + 1) == '-') {
                while (i < script.length() && script.charAt(i) != '\n') {
                    i++;
                }
                clean.append('\n');
                continue;
            }
            clean.append(c);
        }
        List<String> statements = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        inQuote = false;
        for (int i = 0; i < clean.length(); i++) {
            char c = clean.charAt(i);
            if (c == '\'') {
                inQuote = !inQuote;
            }
            if (c == ';' && !inQuote) {
                addIfNotBlank(statements, current);
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        addIfNotBlank(statements, current);
        return statements;
    }

    private static void addIfNotBlank(List<String> statements, StringBuilder sb) {
        String s = sb.toString().trim();
        if (!s.isEmpty()) {
            statements.add(s);
        }
    }
}
