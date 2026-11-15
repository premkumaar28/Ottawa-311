package com.ottawa311.db;

import java.sql.*;
import java.util.Locale;


/**
 * User Data - managing resident authentication and registration.
 * PR0001: Resident registration, user login authentication
 */


public class UserDAO {

    public static class Account {
        public final String email, role, department;
        private final String password;

        public Account(String email, String password, String role, String department) {
            this.email = email;
            this.password = password;
            this.role = role;
            this.department = department;
        }

        public boolean passwordMatches(String candidate) {
            return this.password.equals(candidate);
        }
    }
    
    /**
     * Authenticates a user against the database securely using a parameterized PreparedStatement.
     */
    public Account authenticate(String email, String pass) {
        if (email == null || pass == null) return null;
        String sql = "SELECT email, password_hash, role, department FROM users WHERE LOWER(email) = ?";
        
        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, email.toLowerCase(Locale.ROOT));
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Account acc = new Account(
                        rs.getString("email"),
                        rs.getString("password_hash"),
                        rs.getString("role"),
                        rs.getString("department")
                    );
                    return acc.passwordMatches(pass) ? acc : null;
                }
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during user authentication for email: " + email, e);
        }
		return null;
    }
    
    /**
     * Registers a new resident account safely with parameterized SQL queries.
     */
    public boolean register(String email, String pass) {
        String checkSql = "SELECT COUNT(*) FROM users WHERE LOWER(email) = ?";
        String insertSql = "INSERT INTO users (email, password_hash, role, department) VALUES (?, ?, 'RESIDENT', '')";

        String key = email.toLowerCase(Locale.ROOT);
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql)) {
                checkStmt.setString(1, key);
                try (ResultSet rs = checkStmt.executeQuery()) {
                    if (rs.next() && rs.getInt(1) > 0) return false; // Email exists
                }
            }

            try (PreparedStatement insertStmt = conn.prepareStatement(insertSql)) {
                insertStmt.setString(1, key);
                insertStmt.setString(2, pass);
                return insertStmt.executeUpdate() > 0;
            }
        } catch (SQLException e) {
            throw new RuntimeException("Database error during resident registration for email: " + email, e);
        }
    }
}