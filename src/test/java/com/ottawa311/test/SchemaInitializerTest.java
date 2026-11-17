package com.ottawa311.test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

import com.ottawa311.db.SchemaInitializer;

public class SchemaInitializerTest {

    @Test
    public void splitsOnSemicolons() {
        List<String> s = SchemaInitializer.splitStatements("SELECT 1; SELECT 2;");
        assertEquals(2, s.size());
        assertEquals("SELECT 1", s.get(0));
        assertEquals("SELECT 2", s.get(1));
    }

    @Test
    public void lastStatementWithoutSemicolonIsKept() {
        List<String> s = SchemaInitializer.splitStatements("SELECT 1; SELECT 2");
        assertEquals(2, s.size());
    }

    @Test
    public void removesLineComments() {
        List<String> s = SchemaInitializer.splitStatements("-- header\nSELECT 1; -- trailing\nSELECT 2;");
        assertEquals(2, s.size());
        assertFalse(s.get(0).contains("--"));
        assertFalse(s.get(1).contains("trailing"));
    }

    @Test
    public void semicolonInsideQuotesDoesNotSplit() {
        List<String> s = SchemaInitializer.splitStatements("INSERT INTO t VALUES ('a;b');");
        assertEquals(1, s.size());
        assertTrue(s.get(0).contains("'a;b'"));
    }

    @Test
    public void doubleDashInsideQuotesIsNotAComment() {
        List<String> s = SchemaInitializer.splitStatements("INSERT INTO t VALUES ('x--y');");
        assertEquals(1, s.size());
        assertTrue(s.get(0).contains("'x--y'"));
    }

    @Test
    public void blankInputGivesNoStatements() {
        assertTrue(SchemaInitializer.splitStatements("").isEmpty());
        assertTrue(SchemaInitializer.splitStatements("  \n -- only a comment\n ;; ").isEmpty());
    }

    @Test
    public void realSchemaFileSplitsIntoExpectedStatements() throws Exception {
        try (InputStream in = getClass().getClassLoader().getResourceAsStream("schema.sql")) {
            assertNotNull("schema.sql must be on the classpath", in);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            List<String> s = SchemaInitializer.splitStatements(new String(out.toByteArray(), StandardCharsets.UTF_8));

            // CREATE DATABASE, USE, CREATE TABLE users, CREATE TABLE reports, INSERT demo accounts
            assertEquals(5, s.size());
            assertTrue(s.get(0).toUpperCase().startsWith("CREATE DATABASE"));
            assertTrue(s.get(1).toUpperCase().startsWith("USE "));
            assertTrue(s.get(2).contains("CREATE TABLE IF NOT EXISTS users"));
            assertTrue(s.get(3).contains("CREATE TABLE IF NOT EXISTS reports"));
            assertTrue(s.get(4).toUpperCase().startsWith("INSERT IGNORE"));
        }
    }
}