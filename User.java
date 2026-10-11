package com.college.event.model;

import java.sql.Timestamp;

/**
 * OOP PRINCIPLE DEMONSTRATION:
 * 1. ABSTRACTION: This is an abstract base class defining common user attributes
 *    and declaring abstract methods that subclasses must implement.
 * 2. ENCAPSULATION: All fields are private, with public accessors and mutators
 *    to protect internal state from direct modification.
 */
public abstract class User {
    // Private encapsulated fields
    private int userId;
    private String username;
    private String email;
    private String passwordHash;
    private Role role;
    private String fullName;
    private Timestamp createdAt;

    // Default constructor
    public User() {
    }

    // Parameterized constructor
    public User(int userId, String username, String email, String passwordHash, Role role, String fullName) {
        this.userId = userId;
        this.username = username;
        this.email = email;
        this.passwordHash = passwordHash;
        this.role = role;
        this.fullName = fullName;
    }

    // =========================================================================
    // POLYMORPHISM DEMONSTRATION:
    // Subclasses (Admin, Student) must override these abstract methods.
    // When called through a User reference, runtime polymorphism executes the
    // subclass's specific implementation.
    // =========================================================================

    /**
     * Polymorphic method: Returns a role-specific description.
     */
    public abstract String getRoleDescription();

    /**
     * Polymorphic method: Returns a role-specific dashboard header title.
     */
    public abstract String getDashboardTitle();

    /**
     * Polymorphic method: Evaluates whether this user has administrator privileges.
     */
    public abstract boolean hasAdminPrivileges();

    // =========================================================================
    // Encapsulated Getters and Setters
    // =========================================================================

    public int getUserId() {
        return userId;
    }

    public void setUserId(int userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPasswordHash() {
        return passwordHash;
    }

    public void setPasswordHash(String passwordHash) {
        this.passwordHash = passwordHash;
    }

    public Role getRole() {
        return role;
    }

    public void setRole(Role role) {
        this.role = role;
    }

    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public String toString() {
        return fullName + " (" + role + ")";
    }
}
