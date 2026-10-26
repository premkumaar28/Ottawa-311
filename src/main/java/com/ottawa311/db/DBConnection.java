package com.ottawa311.db;

import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Singleton Pattern for managing MySQL Database Connections.
 * Gets configuration from db.properties and manages MySQL driver setup
 */
public final class DBConnection {
	private static String url;
    private static String user;
    private static String password;

    private static DBConnection instance;

    static {
        Properties props = new Properties();
        try (InputStream in = DBConnection.class.getClassLoader().getResourceAsStream("db.properties")) {
        	
                props.load(in);
                url = props.getProperty("db.url");
                user = props.getProperty("db.user");
                password = props.getProperty("db.password");
                
            Class.forName("com.mysql.cj.jdbc.Driver");
        } catch (Exception e) {
            throw new RuntimeException("Failed to initialize database configuration or JDBC driver", e);
        }
}

    private DBConnection() {}

    public static synchronized DBConnection getInstance() {
        if (instance == null) {
            instance = new DBConnection();
        }
        return instance;
    }

    public Connection getConnection() throws SQLException {
    	Connection conn = DriverManager.getConnection(url, user, password);
        if (conn == null) {
            throw new SQLException("Failed to establish database connection (Connection object returned null).");
        }
        return conn;
    }
}