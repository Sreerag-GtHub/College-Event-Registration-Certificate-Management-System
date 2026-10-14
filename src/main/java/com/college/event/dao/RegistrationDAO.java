package com.college.event.dao;

import com.college.event.model.Registration;
import com.college.event.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class RegistrationDAO {

    public int registerStudentTransactional(int eventId, int studentId, boolean requiresApproval) throws SQLException {
        String lockEventSql = "SELECT max_capacity, status, registration_deadline FROM events WHERE event_id = ? FOR UPDATE";
        String checkDupSql = "SELECT 1 FROM registrations WHERE event_id = ? AND student_id = ? FOR UPDATE";
        String countApprovedSql = "SELECT COUNT(*) FROM registrations WHERE event_id = ? AND status = 'APPROVED'";
        String insertRegSql = "INSERT INTO registrations (event_id, student_id, status) VALUES (?, ?, ?)";
        String insertAttSql = "INSERT INTO attendance (registration_id, status) VALUES (?, 'NOT_RECORDED')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement checkDupStmt = conn.prepareStatement(checkDupSql)) {
                checkDupStmt.setInt(1, eventId);
                checkDupStmt.setInt(2, studentId);
                try (ResultSet rs = checkDupStmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        throw new SQLException("You have already registered for this event.");
                    }
                }
            }

            int maxCapacity = 0;
            String eventStatus = "CLOSED";
            Timestamp deadline = null;

            try (PreparedStatement lockStmt = conn.prepareStatement(lockEventSql)) {
                lockStmt.setInt(1, eventId);
                try (ResultSet rs = lockStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("Event not found (ID: " + eventId + ").");
                    }
                    maxCapacity = rs.getInt("max_capacity");
                    eventStatus = rs.getString("status");
                    deadline = rs.getTimestamp("registration_deadline");
                }
            }

            if (!"OPEN".equalsIgnoreCase(eventStatus)) {
                conn.rollback();
                throw new SQLException("Registration is not open for this event (Current Status: " + eventStatus + ").");
            }

            if (deadline != null && System.currentTimeMillis() > deadline.getTime()) {
                conn.rollback();
                throw new SQLException("The registration deadline for this event has passed.");
            }

            if (maxCapacity > 0) {
                try (PreparedStatement countStmt = conn.prepareStatement(countApprovedSql)) {
                    countStmt.setInt(1, eventId);
                    try (ResultSet rs = countStmt.executeQuery()) {
                        if (rs.next()) {
                            int approved = rs.getInt(1);
                            if (approved >= maxCapacity) {
                                conn.rollback();
                                throw new SQLException("This event has reached its maximum capacity (" + maxCapacity + " seats).");
                            }
                        }
                    }
                }
            }

            String initialStatus = requiresApproval ? "PENDING" : "APPROVED";
            int generatedRegId = -1;

            try (PreparedStatement insertStmt = conn.prepareStatement(insertRegSql, Statement.RETURN_GENERATED_KEYS)) {
                insertStmt.setInt(1, eventId);
                insertStmt.setInt(2, studentId);
                insertStmt.setString(3, initialStatus);
                insertStmt.executeUpdate();

                try (ResultSet keys = insertStmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        generatedRegId = keys.getInt(1);
                    }
                }
            }

            if (generatedRegId <= 0) {
                conn.rollback();
                throw new SQLException("Failed to create registration record.");
            }

            if ("APPROVED".equalsIgnoreCase(initialStatus)) {
                try (PreparedStatement attStmt = conn.prepareStatement(insertAttSql)) {
                    attStmt.setInt(1, generatedRegId);
                    attStmt.executeUpdate();
                }
            }

            conn.commit();
            return generatedRegId;

        } catch (SQLException ex) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw ex;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public boolean updateStatus(int registrationId, String newStatus, String remarks) throws SQLException {
        String updateSql = "UPDATE registrations SET status = ?, remarks = ? WHERE registration_id = ?";
        String initAttSql = "INSERT IGNORE INTO attendance (registration_id, status) VALUES (?, 'NOT_RECORDED')";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement stmt = conn.prepareStatement(updateSql)) {
                stmt.setString(1, newStatus);
                stmt.setString(2, remarks);
                stmt.setInt(3, registrationId);
                int updated = stmt.executeUpdate();
                if (updated == 0) {
                    conn.rollback();
                    return false;
                }
            }

            if ("APPROVED".equalsIgnoreCase(newStatus)) {
                try (PreparedStatement attStmt = conn.prepareStatement(initAttSql)) {
                    attStmt.setInt(1, registrationId);
                    attStmt.executeUpdate();
                }
            }

            conn.commit();
            return true;
        } catch (SQLException e) {
            if (conn != null) {
                try { conn.rollback(); } catch (SQLException ignored) {}
            }
            throw e;
        } finally {
            if (conn != null) {
                try { conn.setAutoCommit(true); conn.close(); } catch (SQLException ignored) {}
            }
        }
    }

    public List<Registration> findByStudentId(int studentId) throws SQLException {
        String sql = "SELECT r.*, e.title AS event_title, e.event_date, "
                   + "COALESCE(a.status, 'NOT_RECORDED') AS att_status, "
                   + "c.certificate_code "
                   + "FROM registrations r "
                   + "JOIN events e ON r.event_id = e.event_id "
                   + "LEFT JOIN attendance a ON r.registration_id = a.registration_id "
                   + "LEFT JOIN certificates c ON r.registration_id = c.registration_id "
                   + "WHERE r.student_id = ? "
                   + "ORDER BY r.registration_date DESC";

        List<Registration> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Registration reg = new Registration();
                    reg.setRegistrationId(rs.getInt("registration_id"));
                    reg.setEventId(rs.getInt("event_id"));
                    reg.setStudentId(rs.getInt("student_id"));
                    reg.setRegistrationDate(rs.getTimestamp("registration_date"));
                    reg.setStatus(rs.getString("status"));
                    reg.setRemarks(rs.getString("remarks"));
                    reg.setEventTitle(rs.getString("event_title"));
                    reg.setEventDate(rs.getTimestamp("event_date"));
                    reg.setAttendanceStatus(rs.getString("att_status"));

                    String certCode = rs.getString("certificate_code");
                    reg.setCertificateCode(certCode);
                    reg.setCertificateIssued(certCode != null);

                    list.add(reg);
                }
            }
        }
        return list;
    }

    public List<Registration> findAllWithDetails(Integer eventIdFilter) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT r.*, e.title AS event_title, e.event_date, "
          + "u.full_name AS student_name, u.student_id AS student_roll_no, u.department, "
          + "COALESCE(a.status, 'NOT_RECORDED') AS att_status, "
          + "c.certificate_code "
          + "FROM registrations r "
          + "JOIN events e ON r.event_id = e.event_id "
          + "JOIN users u ON r.student_id = u.user_id "
          + "LEFT JOIN attendance a ON r.registration_id = a.registration_id "
          + "LEFT JOIN certificates c ON r.registration_id = c.registration_id "
        );

        if (eventIdFilter != null && eventIdFilter > 0) {
            sql.append("WHERE r.event_id = ? ");
        }

        sql.append("ORDER BY r.registration_date DESC");

        List<Registration> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            if (eventIdFilter != null && eventIdFilter > 0) {
                stmt.setInt(1, eventIdFilter);
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Registration reg = new Registration();
                    reg.setRegistrationId(rs.getInt("registration_id"));
                    reg.setEventId(rs.getInt("event_id"));
                    reg.setStudentId(rs.getInt("student_id"));
                    reg.setRegistrationDate(rs.getTimestamp("registration_date"));
                    reg.setStatus(rs.getString("status"));
                    reg.setRemarks(rs.getString("remarks"));
                    reg.setEventTitle(rs.getString("event_title"));
                    reg.setEventDate(rs.getTimestamp("event_date"));
                    reg.setStudentName(rs.getString("student_name"));
                    reg.setStudentRollNo(rs.getString("student_roll_no"));
                    reg.setDepartment(rs.getString("department"));
                    reg.setAttendanceStatus(rs.getString("att_status"));

                    String certCode = rs.getString("certificate_code");
                    reg.setCertificateCode(certCode);
                    reg.setCertificateIssued(certCode != null);

                    list.add(reg);
                }
            }
        }
        return list;
    }

    public Registration findById(int registrationId) throws SQLException {
        String sql = "SELECT r.*, e.title AS event_title, e.event_date, "
                   + "u.full_name AS student_name, u.student_id AS student_roll_no, u.department, "
                   + "COALESCE(a.status, 'NOT_RECORDED') AS att_status, "
                   + "c.certificate_code "
                   + "FROM registrations r "
                   + "JOIN events e ON r.event_id = e.event_id "
                   + "JOIN users u ON r.student_id = u.user_id "
                   + "LEFT JOIN attendance a ON r.registration_id = a.registration_id "
                   + "LEFT JOIN certificates c ON r.registration_id = c.registration_id "
                   + "WHERE r.registration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, registrationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    Registration reg = new Registration();
                    reg.setRegistrationId(rs.getInt("registration_id"));
                    reg.setEventId(rs.getInt("event_id"));
                    reg.setStudentId(rs.getInt("student_id"));
                    reg.setRegistrationDate(rs.getTimestamp("registration_date"));
                    reg.setStatus(rs.getString("status"));
                    reg.setRemarks(rs.getString("remarks"));
                    reg.setEventTitle(rs.getString("event_title"));
                    reg.setEventDate(rs.getTimestamp("event_date"));
                    reg.setStudentName(rs.getString("student_name"));
                    reg.setStudentRollNo(rs.getString("student_roll_no"));
                    reg.setDepartment(rs.getString("department"));
                    reg.setAttendanceStatus(rs.getString("att_status"));

                    String certCode = rs.getString("certificate_code");
                    reg.setCertificateCode(certCode);
                    reg.setCertificateIssued(certCode != null);
                    return reg;
                }
            }
        }
        return null;
    }

    public boolean isStudentRegistered(int eventId, int studentId) throws SQLException {
        String sql = "SELECT 1 FROM registrations WHERE event_id = ? AND student_id = ? LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            stmt.setInt(2, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countTotalRegistrations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    public int countApprovedRegistrations() throws SQLException {
        String sql = "SELECT COUNT(*) FROM registrations WHERE status = 'APPROVED'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
