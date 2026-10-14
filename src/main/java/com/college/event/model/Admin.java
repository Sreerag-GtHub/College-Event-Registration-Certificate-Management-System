package com.college.event.model;

public class Admin extends User {

    public Admin() {
        super();
        setRole(Role.ADMIN);
    }

    public Admin(int userId, String username, String email, String passwordHash, String fullName) {
        super(userId, username, email, passwordHash, Role.ADMIN, fullName);
    }

    @Override
    public String getRoleDescription() {
        return "Administrator - manage events, registrations, attendance and certificates";
    }

    @Override
    public String getDashboardTitle() {
        return "Admin Dashboard - " + getFullName();
    }

    @Override
    public boolean hasAdminPrivileges() {
        return true;
    }
}
