package com.college.event.dao;

import com.college.event.model.Attendance;
import com.college.event.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class AttendanceDAO {

    public boolean markAttendance(int registrationId, String status, int markedByUserId, String remarks) throws SQLException {
        String sql = "INSERT INTO attendance (registration_id, status, marked_by, marked_at, remarks) "
                   + "VALUES (?, ?, ?, CURRENT_TIMESTAMP, ?) "
                   + "ON DUPLICATE KEY UPDATE status = VALUES(status), marked_by = VALUES(marked_by), "
                   + "marked_at = CURRENT_TIMESTAMP, remarks = VALUES(remarks)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, registrationId);
            stmt.setString(2, status);
            stmt.setInt(3, markedByUserId);
            stmt.setString(4, remarks);

            return stmt.executeUpdate() > 0;
        }
    }

    public List<Attendance> findParticipantsByEvent(int eventId) throws SQLException {
        String sql = "SELECT r.registration_id, "
                   + "COALESCE(a.attendance_id, 0) AS attendance_id, "
                   + "COALESCE(a.status, 'NOT_RECORDED') AS att_status, "
                   + "a.marked_by, a.marked_at, a.remarks, "
                   + "u.full_name AS student_name, u.student_id AS student_roll_no, u.department, "
                   + "e.title AS event_title, e.event_date "
                   + "FROM registrations r "
                   + "JOIN users u ON r.student_id = u.user_id "
                   + "JOIN events e ON r.event_id = e.event_id "
                   + "LEFT JOIN attendance a ON r.registration_id = a.registration_id "
                   + "WHERE r.event_id = ? AND r.status = 'APPROVED' "
                   + "ORDER BY u.full_name ASC";

        List<Attendance> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Attendance att = new Attendance();
                    att.setRegistrationId(rs.getInt("registration_id"));
                    att.setAttendanceId(rs.getInt("attendance_id"));
                    att.setStatus(rs.getString("att_status"));

                    int markedBy = rs.getInt("marked_by");
                    if (!rs.wasNull()) {
                        att.setMarkedBy(markedBy);
                    }
                    att.setMarkedAt(rs.getTimestamp("marked_at"));
                    att.setRemarks(rs.getString("remarks"));

                    att.setStudentName(rs.getString("student_name"));
                    att.setStudentRollNo(rs.getString("student_roll_no"));
                    att.setDepartment(rs.getString("department"));
                    att.setEventTitle(rs.getString("event_title"));
                    att.setEventDate(rs.getTimestamp("event_date"));

                    list.add(att);
                }
            }
        }
        return list;
    }

    public boolean isStudentPresent(int registrationId) throws SQLException {
        String sql = "SELECT 1 FROM attendance WHERE registration_id = ? AND status = 'PRESENT' LIMIT 1";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, registrationId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next();
            }
        }
    }

    public int countStudentsPresent() throws SQLException {
        String sql = "SELECT COUNT(*) FROM attendance WHERE status = 'PRESENT'";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
}
