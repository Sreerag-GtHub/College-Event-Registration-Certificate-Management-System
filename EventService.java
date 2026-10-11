package com.college.event.service;

import com.college.event.dao.AttendanceDAO;
import com.college.event.dao.EventDAO;
import com.college.event.dao.RegistrationDAO;
import com.college.event.dao.UserDAO;
import com.college.event.model.Event;
import com.college.event.model.User;
import com.college.event.util.ValidationUtil;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service managing college events, administrative CRUD operations, and event statistics.
 * Enforces role-based authorization in service methods.
 */
public class EventService {

    private final EventDAO eventDAO;
    private final UserDAO userDAO;
    private final RegistrationDAO registrationDAO;
    private final AttendanceDAO attendanceDAO;

    public EventService() {
        this.eventDAO = new EventDAO();
        this.userDAO = new UserDAO();
        this.registrationDAO = new RegistrationDAO();
        this.attendanceDAO = new AttendanceDAO();
    }

    public EventService(EventDAO eventDAO, UserDAO userDAO, RegistrationDAO registrationDAO, AttendanceDAO attendanceDAO) {
        this.eventDAO = eventDAO;
        this.userDAO = userDAO;
        this.registrationDAO = registrationDAO;
        this.attendanceDAO = attendanceDAO;
    }

    /**
     * Creates a new event.
     * Enforces Administrator authorization.
     */
    public boolean createEvent(User currentUser, Event event) throws SecurityException, IllegalArgumentException, SQLException {
        assertAdmin(currentUser);
        validateEvent(event);

        event.setCreatedBy(currentUser.getUserId());
        return eventDAO.createEvent(event);
    }

    /**
     * Updates an existing event.
     * Enforces Administrator authorization.
     */
    public boolean updateEvent(User currentUser, Event event) throws SecurityException, IllegalArgumentException, SQLException {
        assertAdmin(currentUser);
        if (event.getEventId() <= 0) {
            throw new IllegalArgumentException("Invalid Event ID for update.");
        }
        validateEvent(event);

        return eventDAO.updateEvent(event);
    }

    /**
     * Changes event registration status (OPEN, CLOSED, COMPLETED, CANCELLED).
     */
    public boolean changeEventStatus(User currentUser, int eventId, String newStatus) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        if (eventId <= 0 || !ValidationUtil.isNonEmpty(newStatus)) {
            throw new IllegalArgumentException("Event ID and status must be valid.");
        }
        return eventDAO.updateStatus(eventId, newStatus.toUpperCase());
    }

    /**
     * Deletes an event.
     * Enforces Administrator authorization.
     */
    public boolean deleteEvent(User currentUser, int eventId) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        if (eventId <= 0) {
            throw new IllegalArgumentException("Invalid Event ID.");
        }
        return eventDAO.deleteEvent(eventId);
    }

    public Event getEventById(int eventId) throws SQLException {
        return eventDAO.findById(eventId);
    }

    public List<Event> getAllEvents() throws SQLException {
        return eventDAO.findAllEvents();
    }

    public List<Event> searchEvents(String keyword, String category, boolean onlyOpen) throws SQLException {
        return eventDAO.searchAndFilter(keyword, category, onlyOpen);
    }

    /**
     * Retrieves aggregated database statistics for the Administrator control panel.
     */
    public Map<String, Integer> getDashboardStatistics(User currentUser) throws SecurityException, SQLException {
        assertAdmin(currentUser);

        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalStudents", userDAO.countStudents());
        stats.put("totalEvents", eventDAO.countEvents());
        stats.put("totalRegistrations", registrationDAO.countTotalRegistrations());
        stats.put("approvedRegistrations", registrationDAO.countApprovedRegistrations());
        stats.put("studentsPresent", attendanceDAO.countStudentsPresent());

        return stats;
    }

    // =========================================================================
    // Private Helpers
    // =========================================================================

    private void assertAdmin(User currentUser) {
        if (currentUser == null || !currentUser.hasAdminPrivileges()) {
            throw new SecurityException("Unauthorized: Administrator permissions are required for this action.");
        }
    }

    private void validateEvent(Event event) {
        if (event == null) {
            throw new IllegalArgumentException("Event data cannot be null.");
        }
        if (!ValidationUtil.isNonEmpty(event.getTitle())) {
            throw new IllegalArgumentException("Event title is required.");
        }
        if (!ValidationUtil.isNonEmpty(event.getCategory())) {
            throw new IllegalArgumentException("Event category is required.");
        }
        if (!ValidationUtil.isNonEmpty(event.getVenue())) {
            throw new IllegalArgumentException("Venue is required.");
        }
        if (event.getEventDate() == null) {
            throw new IllegalArgumentException("Event date and time is required.");
        }
        if (event.getRegistrationDeadline() == null) {
            throw new IllegalArgumentException("Registration deadline is required.");
        }
        if (!ValidationUtil.isDeadlineBeforeEventDate(event.getRegistrationDeadline(), event.getEventDate())) {
            throw new IllegalArgumentException("Registration deadline must be before the event date.");
        }
        if (event.getMaxCapacity() <= 0) {
            throw new IllegalArgumentException("Maximum capacity must be a positive number greater than 0.");
        }
    }
}
