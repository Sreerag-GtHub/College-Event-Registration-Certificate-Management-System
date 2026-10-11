package com.college.event.service;

import com.college.event.dao.AttendanceDAO;
import com.college.event.model.Attendance;
import com.college.event.model.User;

import java.sql.SQLException;
import java.util.List;

/**
 * Service managing event attendance recording.
 * Enforces Administrator authorization.
 */
public class AttendanceService {

    private final AttendanceDAO attendanceDAO;

    public AttendanceService() {
        this.attendanceDAO = new AttendanceDAO();
    }

    public AttendanceService(AttendanceDAO attendanceDAO) {
        this.attendanceDAO = attendanceDAO;
    }

    /**
     * Records student attendance as PRESENT or ABSENT.
     * Enforces Administrator permissions.
     */
    public boolean markAttendance(User currentUser, int registrationId, String status, String remarks) throws SecurityException, SQLException {
        assertAdmin(currentUser);

        if (!"PRESENT".equalsIgnoreCase(status) && !"ABSENT".equalsIgnoreCase(status)) {
            throw new IllegalArgumentException("Attendance status must be either 'PRESENT' or 'ABSENT'.");
        }

        return attendanceDAO.markAttendance(registrationId, status.toUpperCase(), currentUser.getUserId(), remarks);
    }

    /**
     * Retrieves all approved participants and attendance records for an event.
     */
    public List<Attendance> getEventParticipants(User currentUser, int eventId) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        return attendanceDAO.findParticipantsByEvent(eventId);
    }

    private void assertAdmin(User currentUser) {
        if (currentUser == null || !currentUser.hasAdminPrivileges()) {
            throw new SecurityException("Unauthorized: Administrator permissions are required.");
        }
    }
}
