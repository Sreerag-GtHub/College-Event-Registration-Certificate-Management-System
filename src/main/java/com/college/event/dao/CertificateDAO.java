package com.college.event.dao;

import com.college.event.model.Certificate;
import com.college.event.model.Registration;
import com.college.event.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class CertificateDAO {

    public boolean issueCertificate(Certificate cert) throws SQLException {
        String verifySql = "SELECT r.status AS reg_status, a.status AS att_status, e.certificate_eligible "
                         + "FROM registrations r "
                         + "JOIN events e ON r.event_id = e.event_id "
                         + "LEFT JOIN attendance a ON r.registration_id = a.registration_id "
                         + "WHERE r.registration_id = ?";

        String checkDupSql = "SELECT 1 FROM certificates WHERE student_id = ? AND event_id = ?";
        String insertSql = "INSERT INTO certificates (certificate_code, registration_id, student_id, event_id, issue_date, issued_by, pdf_path) "
                         + "VALUES (?, ?, ?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DatabaseConnection.getConnection();
            conn.setAutoCommit(false);

            try (PreparedStatement vStmt = conn.prepareStatement(verifySql)) {
                vStmt.setInt(1, cert.getRegistrationId());
                try (ResultSet rs = vStmt.executeQuery()) {
                    if (!rs.next()) {
                        conn.rollback();
                        throw new SQLException("Registration record not found.");
                    }

                    String regStatus = rs.getString("reg_status");
                    String attStatus = rs.getString("att_status");
                    boolean certEligible = rs.getBoolean("certificate_eligible");

                    if (!"APPROVED".equalsIgnoreCase(regStatus)) {
                        conn.rollback();
                        throw new SQLException("Cannot issue certificate: Registration is not APPROVED (Status: " + regStatus + ").");
                    }

                    if (!"PRESENT".equalsIgnoreCase(attStatus)) {
                        conn.rollback();
                        throw new SQLException("Cannot issue certificate: Attendance is not marked PRESENT (Status: " + attStatus + ").");
                    }

                    if (!certEligible) {
                        conn.rollback();
                        throw new SQLException("This event is not configured for participation certificate issuance.");
                    }
                }
            }

            try (PreparedStatement dupStmt = conn.prepareStatement(checkDupSql)) {
                dupStmt.setInt(1, cert.getStudentId());
                dupStmt.setInt(2, cert.getEventId());
                try (ResultSet rs = dupStmt.executeQuery()) {
                    if (rs.next()) {
                        conn.rollback();
                        throw new SQLException("A certificate has already been issued to this student for this event.");
                    }
                }
            }

            try (PreparedStatement insStmt = conn.prepareStatement(insertSql, Statement.RETURN_GENERATED_KEYS)) {
                insStmt.setString(1, cert.getCertificateCode());
                insStmt.setInt(2, cert.getRegistrationId());
                insStmt.setInt(3, cert.getStudentId());
                insStmt.setInt(4, cert.getEventId());
                insStmt.setTimestamp(5, cert.getIssueDate());

                if (cert.getIssuedBy() != null) {
                    insStmt.setInt(6, cert.getIssuedBy());
                } else {
                    insStmt.setNull(6, Types.INTEGER);
                }

                insStmt.setString(7, cert.getPdfPath());

                int rows = insStmt.executeUpdate();
                if (rows > 0) {
                    try (ResultSet keys = insStmt.getGeneratedKeys()) {
                        if (keys.next()) {
                            cert.setCertificateId(keys.getInt(1));
                        }
                    }
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

    public Certificate findByRegistrationId(int registrationId) throws SQLException {
        String sql = "SELECT c.*, u.full_name AS student_name, u.student_id AS student_roll_no, u.department, "
                   + "e.title AS event_title, e.category AS event_category, e.event_date "
                   + "FROM certificates c "
                   + "JOIN users u ON c.student_id = u.user_id "
                   + "JOIN events e ON c.event_id = e.event_id "
                   + "WHERE c.registration_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, registrationId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToCertificate(rs);
                }
            }
        }
        return null;
    }

    public List<Certificate> findByStudentId(int studentId) throws SQLException {
        String sql = "SELECT c.*, u.full_name AS student_name, u.student_id AS student_roll_no, u.department, "
                   + "e.title AS event_title, e.category AS event_category, e.event_date "
                   + "FROM certificates c "
                   + "JOIN users u ON c.student_id = u.user_id "
                   + "JOIN events e ON c.event_id = e.event_id "
                   + "WHERE c.student_id = ? "
                   + "ORDER BY c.issue_date DESC";

        List<Certificate> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, studentId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToCertificate(rs));
                }
            }
        }
        return list;
    }

    public List<Registration> findEligibleParticipants(int eventId) throws SQLException {
        String sql = "SELECT r.registration_id, r.event_id, r.student_id, r.status, "
                   + "u.full_name AS student_name, u.student_id AS student_roll_no, u.department, "
                   + "e.title AS event_title, e.event_date, "
                   + "COALESCE(a.status, 'NOT_RECORDED') AS att_status, "
                   + "c.certificate_code "
                   + "FROM registrations r "
                   + "JOIN users u ON r.student_id = u.user_id "
                   + "JOIN events e ON r.event_id = e.event_id "
                   + "JOIN attendance a ON r.registration_id = a.registration_id "
                   + "LEFT JOIN certificates c ON r.registration_id = c.registration_id "
                   + "WHERE r.event_id = ? AND r.status = 'APPROVED' AND a.status = 'PRESENT' "
                   + "ORDER BY u.full_name ASC";

        List<Registration> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Registration reg = new Registration();
                    reg.setRegistrationId(rs.getInt("registration_id"));
                    reg.setEventId(rs.getInt("event_id"));
                    reg.setStudentId(rs.getInt("student_id"));
                    reg.setStatus(rs.getString("status"));
                    reg.setStudentName(rs.getString("student_name"));
                    reg.setStudentRollNo(rs.getString("student_roll_no"));
                    reg.setDepartment(rs.getString("department"));
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

    public int countCertificatesIssued() throws SQLException {
        String sql = "SELECT COUNT(*) FROM certificates";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Certificate mapRowToCertificate(ResultSet rs) throws SQLException {
        Certificate cert = new Certificate();
        cert.setCertificateId(rs.getInt("certificate_id"));
        cert.setCertificateCode(rs.getString("certificate_code"));
        cert.setRegistrationId(rs.getInt("registration_id"));
        cert.setStudentId(rs.getInt("student_id"));
        cert.setEventId(rs.getInt("event_id"));
        cert.setIssueDate(rs.getTimestamp("issue_date"));

        int issuedBy = rs.getInt("issued_by");
        if (!rs.wasNull()) {
            cert.setIssuedBy(issuedBy);
        }
        cert.setPdfPath(rs.getString("pdf_path"));

        cert.setStudentName(rs.getString("student_name"));
        cert.setStudentRollNo(rs.getString("student_roll_no"));
        cert.setDepartment(rs.getString("department"));
        cert.setEventTitle(rs.getString("event_title"));
        cert.setEventCategory(rs.getString("event_category"));
        cert.setEventDate(rs.getTimestamp("event_date"));

        return cert;
    }
}
