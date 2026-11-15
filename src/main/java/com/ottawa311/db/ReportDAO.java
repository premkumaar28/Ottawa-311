package com.ottawa311.db;

import java.sql.*;
import java.time.Instant;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;


/**
 * Report Data Access - managing infrastructure reports and status tracking.
 * Implements database operations for:
 * PR0002, PR0003, PR0004, PR0005,PR0006, PR0007, PR0008, PR0009, PR0010
 */

public class ReportDAO {
	
    private static final Logger LOG = Logger.getLogger(ReportDAO.class.getName());

    private static final String COLUMNS = "report_id, owner_email, location, category, description, "
            + "urgency, photo_name, department, status, notes, submitted_at, closed_at";

    private static final String SQL_INSERT =
            "INSERT INTO reports (report_id, owner_email, location, category, description, "
            + "urgency, photo_name, department, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, 'SUBMITTED')";
    private static final String SQL_GET = "SELECT " + COLUMNS + " FROM reports WHERE report_id = ?";
    private static final String SQL_LIST = "SELECT " + COLUMNS
            + " FROM reports ORDER BY (urgency = 'Urgent') DESC, submitted_at ASC, report_id ASC";
    private static final String SQL_UPDATE =
            "UPDATE reports SET status = ?, notes = ?, closed_at = ? WHERE report_id = ?";
    private static final String SQL_REASSIGN = "UPDATE reports SET department = ? WHERE report_id = ?";
    private static final String SQL_NEXT_ID =
            "SELECT COALESCE(MAX(CAST(SUBSTRING(report_id, 5) AS UNSIGNED)), 1000) + 1 FROM reports";

    private static final int MYSQL_DUPLICATE_ENTRY = 1062;
    private static final int MAX_ID_ATTEMPTS = 5;
    
    /** A report as stored in the database**/
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

        try (Connection conn = DBConnection.getInstance().getConnection()) {
            for (int attempt = 1; attempt <= MAX_ID_ATTEMPTS; attempt++) {
                String id = nextTrackingNumber(conn);
                try (PreparedStatement stmt = conn.prepareStatement(SQL_INSERT)) {
                    stmt.setString(1, id);
                    stmt.setString(2, owner);
                    stmt.setString(3, location);
                    stmt.setString(4, category);
                    stmt.setString(5, description);
                    stmt.setString(6, urgency);
                    stmt.setString(7, photoName == null ? "" : photoName);
                    stmt.setString(8, department);
                    stmt.executeUpdate();
                    return find(conn, id);
                } catch (SQLException e) {
                    if (e.getErrorCode() != MYSQL_DUPLICATE_ENTRY) {
                        throw e;
                    }
                    LOG.log(Level.FINE, "Tracking number {0} already taken, retrying", id);
                }
            }
            throw new DataAccessException("Could not allocate a unique tracking number");
        } catch (SQLException e) {
            throw new DataAccessException("Failed to save report to database", e);
        }
    }
    
    /** Next tracking number: one more than the highest OTT-number stored, starting at OTT-1001. */
    private String nextTrackingNumber(Connection conn) throws SQLException {
        try (Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(SQL_NEXT_ID)) {
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
        try (Connection conn = DBConnection.getInstance().getConnection()) {
            return find(conn, id);
        } catch (SQLException e) {
            throw new DataAccessException("Failed to get report from database", e);
        }
    }

    private Report find(Connection conn, String id) throws SQLException {
        try (PreparedStatement stmt = conn.prepareStatement(SQL_GET)) {
            stmt.setString(1, id);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapResultSetToReport(rs) : null;
            }
        }
    }

    /**
     * PR0007, PR0008, PR0010: Queries all reports sorted by urgency first, then oldest submission timestamp.
     *
     * @return List of all Report objects ordered for triage
     * @throws SQLException if a database access error occurs
     */
    public List<Report> list() throws SQLException {
    	
    	 List<Report> reports = new ArrayList<>();
         try (Connection conn = DBConnection.getInstance().getConnection();
              Statement stmt = conn.createStatement();
              ResultSet rs = stmt.executeQuery(SQL_LIST)) {
             while (rs.next()) {
                 reports.add(mapResultSetToReport(rs));
             }
         } catch (SQLException e) {
             throw new DataAccessException("Failed to list reports from database", e);
         }
         return reports;
    }
    
    /**
     * PR0009: Updates report status (SUBMITTED, IN_PROGRESS, RESOLVED) and worker notes in MySQL.
     *
     * @param r      The Report object to update
     * @param status New workflow status
     * @param note   Worker/Supervisor resolution or progress note
     */
    public void update(Report r, String status, String note) {
    	
    	String safeNote = note == null ? "" : note;
        Instant existingClosed = r.closed;
        Timestamp closedTime = null;
        if ("RESOLVED".equals(status)) {
            closedTime = Timestamp.from(existingClosed != null ? existingClosed : Instant.now());
        }

        try (Connection conn = DBConnection.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(SQL_UPDATE)) {
            stmt.setString(1, status);
            stmt.setString(2, safeNote);
            stmt.setTimestamp(3, closedTime);
            stmt.setString(4, r.id);
            stmt.executeUpdate();

            r.status = status;
            r.note = safeNote;
            r.closed = closedTime != null ? closedTime.toInstant() : null;
        } catch (SQLException e) {
            throw new DataAccessException("Failed to update report " + r.id, e);
        }
    }
    
    /**
     * PR0010: Reassigns a report's responsible department (Roads or Sidewalks) for supervisor triage.
     *
     * @param r          The Report object to reassign
     * @param department New target department
     */
    public void reassign(Report r, String department) {

    	 try (Connection conn = DBConnection.getInstance().getConnection();
                 PreparedStatement stmt = conn.prepareStatement(SQL_REASSIGN)) {
                stmt.setString(1, department);
                stmt.setString(2, r.id);
                stmt.executeUpdate();
                r.department = department;
            } catch (SQLException e) {
                throw new DataAccessException("Failed to reassign report " + r.id, e);
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