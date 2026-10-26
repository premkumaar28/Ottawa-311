package com.ottawa311.db;

import java.sql.*;
import java.time.Instant;
import java.util.*;

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

    public Report create(String owner, String location, String category, String description, String urgency, String photoName) {
        String id = "OTT-" + System.currentTimeMillis() % 100000;
        String department = "Sidewalk".equals(category) ? "Sidewalks" : "Roads";

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