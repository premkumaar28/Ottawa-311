package com.ottawa311.db;

import java.sql.*;
import java.time.Instant;
import java.util.*;


/**
 * Report Data Access - managing infrastructure reports and status tracking.
 * Implements database operations for:
 * PR0002, PR0003, PR0004, PR0005,PR0006, PR0007, PR0008, PR0009, PR0010
 */

public class ReportDAO {

    public static class Report {
        public final String id, owner, location, category, description, urgency, photoName;
        public volatile String department, status, note;
        public final Instant submitted;
        public volatile Instant closed;

        public Report(String id, String owner, String location, String category, String description,
                      String urgency, String photoName, String department, String status,
                      String note, Instant submitted, Instant closed) {
            this.id = id;
            this.owner = owner;
            this.location = location;
            this.category = category;
            this.description = description;
            this.urgency = urgency;
            this.photoName = photoName;
            this.department = department;
            this.status = status;
            this.note = note != null ? note : "";
            this.submitted = submitted;
            this.closed = closed;
        }
    }
    
    /**
     * PR0002, PR0003: Inserts a new infrastructure report into MySQL using the
     * department resolved by the ReportRouter Strategy Pattern.
     *
     * @param owner       Resident email address
     * @param location    Street address or intersection
     * @param category    Report category (e.g., Road, Sidewalk)
     * @param description Detailed description of the issue
     * @param urgency     Urgency level (Routine, Urgent)
     * @param photoName   Stored random filename of uploaded photo (or empty string)
     * @param department  Target department resolved via ReportRouter
     * @return The persisted Report object
     */
    public Report create(String owner, String location, String category, String description, String urgency, String photoName, String department) {
        String id = "OTT-" + System.currentTimeMillis() % 100000;

        String sql = "INSERT INTO reports (report_id, owner_email, location, category, description, urgency, photo_name, department, status) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED')";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            stmt.setString(2, owner);
            stmt.setString(3, location);
            stmt.setString(4, category);
            stmt.setString(5, description);
            stmt.setString(6, urgency);
            stmt.setString(7, photoName);
            stmt.setString(8, department);

            stmt.executeUpdate();
            return get(id);
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to save report to database", e);
        }
    }
    
    /** Next tracking number: one more than the highest OTT-number stored, starting at OTT-1001. */
    private String nextTrackingNumber(Connection conn) throws SQLException {
        String sql = "SELECT COALESCE(MAX(CAST(SUBSTRING(report_id, 5) AS UNSIGNED)), 1000) + 1 FROM reports";
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            rs.next();
            return "OTT-" + rs.getLong(1);
        }
    }
    
    /**
     * PR0004, PR0005: Fetches a single report by tracking ID for detail view or photo viewing access control.
     *
     * @param id Report tracking ID
     * @return The matching Report object, or null if not found
     */
    public Report get(String id) {
        if (id == null) return null;
        String sql = "SELECT * FROM reports WHERE report_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReport(rs);
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to get report from database", e);
        }
        return null;
    }
    
    /**
     * PR0007, PR0008, PR0010: Queries all reports sorted by urgency first, then oldest submission timestamp.
     *
     * @return List of all Report objects ordered for triage
     * @throws SQLException if a database access error occurs
     */
    public List<Report> list() throws SQLException {
        List<Report> out = new ArrayList<>();
        String sql = "SELECT * FROM reports ORDER BY (urgency = 'Urgent') DESC, submitted_at ASC";

        try (Connection conn = DBConnection.getInstance().getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {

            while (rs.next()) {
                out.add(mapResultSetToReport(rs));
            }
        }
        return out;
    }
    
    /**
     * PR0009: Updates report status (SUBMITTED, IN_PROGRESS, RESOLVED) and worker notes in MySQL.
     *
     * @param r      The Report object to update
     * @param status New workflow status
     * @param note   Worker/Supervisor resolution or progress note
     */
    public void update(Report r, String status, String note) {
        String sql = "UPDATE reports SET status = ?, notes = ?, closed_at = ? WHERE report_id = ?";
        Timestamp closedTime = "RESOLVED".equals(status) ? Timestamp.from(Instant.now()) : null;

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, status);
            stmt.setString(2, note);
            stmt.setTimestamp(3, closedTime);
            stmt.setString(4, r.id);

            stmt.executeUpdate();
            r.status = status;
            r.note = note;
            r.closed = closedTime != null ? closedTime.toInstant() : null;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to update report to database", e);
        }
    }
    
    /**
     * PR0010: Reassigns a report's responsible department (Roads or Sidewalks) for supervisor triage.
     *
     * @param r          The Report object to reassign
     * @param department New target department
     */
    public void reassign(Report r, String department) {
        String sql = "UPDATE reports SET department = ? WHERE report_id = ?";

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, department);
            stmt.setString(2, r.id);
            stmt.executeUpdate();
            r.department = department;
        } catch (SQLException e) {
            e.printStackTrace();
            throw new RuntimeException("Failed to reassign report to database", e);
        }
    }
    
    /**
     * Maps a JDBC ResultSet row into an immutable/thread-safe Report domain object.
     */
    private Report mapResultSetToReport(ResultSet rs) throws SQLException {
        Timestamp submittedTs = rs.getTimestamp("submitted_at");
        Timestamp closedTs = rs.getTimestamp("closed_at");

        return new Report(
            rs.getString("report_id"),
            rs.getString("owner_email"),
            rs.getString("location"),
            rs.getString("category"),
            rs.getString("description"),
            rs.getString("urgency"),
            rs.getString("photo_name"),
            rs.getString("department"),
            rs.getString("status"),
            rs.getString("notes"),
            submittedTs != null ? submittedTs.toInstant() : Instant.now(),
            closedTs != null ? closedTs.toInstant() : null
        );
    }
}