package com.college.event.model;

/**
 * OOP PRINCIPLE DEMONSTRATION:
 * 1. INHERITANCE: Admin extends User, reusing identity and credential attributes.
 * 2. POLYMORPHISM: Overrides getRoleDescription(), getDashboardTitle(), and hasAdminPrivileges()
 *    to provide administrator-specific behavior when accessed via a polymorphic User reference.
 */
public class Admin extends User {

    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    public Admin(int userId, String username, String email, String passwordHash, String fullName) {
        super(userId, username, email, passwordHash, Role.ADMIN, fullName);
    }

    // =========================================================================
    // POLYMORPHIC METHOD OVERRIDES
    // =========================================================================

    @Override
    public String getRoleDescription() {
        return "System Administrator with full management control over events, registrations, attendance, and certificates.";
    }

    @Override
    public String getDashboardTitle() {
        return "Administrator Control Center - " + getFullName();
    }

    @Override
    public boolean hasAdminPrivileges() {
        return true;
    }
}
