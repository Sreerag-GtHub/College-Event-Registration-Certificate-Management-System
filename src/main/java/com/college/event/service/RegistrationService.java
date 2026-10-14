package com.college.event.service;

import com.college.event.dao.EventDAO;
import com.college.event.dao.RegistrationDAO;
import com.college.event.model.Event;
import com.college.event.model.Registration;
import com.college.event.model.Student;
import com.college.event.model.User;

import java.sql.SQLException;
import java.util.List;

public class RegistrationService {

    private final RegistrationDAO registrationDAO;
    private final EventDAO eventDAO;

    public RegistrationService() {
        this.registrationDAO = new RegistrationDAO();
        this.eventDAO = new EventDAO();
    }

    public RegistrationService(RegistrationDAO registrationDAO, EventDAO eventDAO) {
        this.registrationDAO = registrationDAO;
        this.eventDAO = eventDAO;
    }

    public int registerForEvent(User currentUser, int eventId) throws SecurityException, SQLException {
        if (currentUser == null || !(currentUser instanceof Student)) {
            throw new SecurityException("Only enrolled students can register for events.");
        }

        Event event = eventDAO.findById(eventId);
        if (event == null) {
            throw new SQLException("Event not found (ID: " + eventId + ").");
        }

        return registrationDAO.registerStudentTransactional(eventId, currentUser.getUserId(), event.isRequiresApproval());
    }

    public boolean approveRegistration(User currentUser, int registrationId, String remarks) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        return registrationDAO.updateStatus(registrationId, "APPROVED", remarks);
    }

    public boolean rejectRegistration(User currentUser, int registrationId, String remarks) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        return registrationDAO.updateStatus(registrationId, "REJECTED", remarks);
    }

    public List<Registration> getStudentRegistrations(User currentUser) throws SecurityException, SQLException {
        if (currentUser == null || !(currentUser instanceof Student)) {
            throw new SecurityException("Only students can view their personal registration history.");
        }
        return registrationDAO.findByStudentId(currentUser.getUserId());
    }

    public List<Registration> getAllRegistrations(User currentUser, Integer eventIdFilter) throws SecurityException, SQLException {
        assertAdmin(currentUser);
        return registrationDAO.findAllWithDetails(eventIdFilter);
    }

    private void assertAdmin(User currentUser) {
        if (currentUser == null || !currentUser.hasAdminPrivileges()) {
            throw new SecurityException("Unauthorized: Administrator permissions are required.");
        }
    }
}
