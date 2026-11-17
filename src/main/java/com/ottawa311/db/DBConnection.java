package com.ottawa311.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton Pattern for managing MySQL database connections.
 * Settings come from db.properties and can be overridden with Java system properties
 * (-Dottawa311.db.url / .user / .password) or environment variables
 * (OTTAWA311_DB_URL / _USER / _PASSWORD).
 */
public final class DBConnection {
    private static final String URL;
    private static final String USER;
    private static final String PASSWORD;

    private static DBConnection instance;

    static {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
            if (in == null) {
                throw new IllegalStateException("db.properties was not found on the classpath");
            }
            props.load(in);
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database configuration or JDBC driver", e);
        }
        URL = setting("db.url", "ottawa311.db.url", "OTTAWA311_DB_URL", props);
        USER = setting("db.user", "ottawa311.db.user", "OTTAWA311_DB_USER", props);
        PASSWORD = setting("db.password", "ottawa311.db.password", "OTTAWA311_DB_PASSWORD", props);
    }

    private DBConnection() {
    }

    /** System property wins, then environment variable, then db.properties. */
    private static String setting(String key, String systemProperty, String envVar, Properties props) {
        String value = System.getProperty(systemProperty);
        if (value == null || value.isEmpty()) {
            value = System.getenv(envVar);
        }
        if (value == null || value.isEmpty()) {
            value = props.getProperty(key);
        }
        return value;
    }

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
        return DriverManager.getConnection(URL, USER, PASSWORD);
    }
}
