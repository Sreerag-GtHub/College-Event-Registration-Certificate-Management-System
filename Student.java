package com.college.event.model;

/**
 * OOP PRINCIPLE DEMONSTRATION:
 * 1. INHERITANCE: Student extends User to inherit common user fields
 *    (userId, username, email, passwordHash, fullName, createdAt).
 * 2. ENCAPSULATION: Additional student-specific fields (studentId, department, course)
 *    are private with public getters and setters.
 * 3. POLYMORPHISM: Overrides getRoleDescription(), getDashboardTitle(), and hasAdminPrivileges().
 */
public class Student extends User {
    // Encapsulated student-specific attributes
    private String studentId; // Unique College Roll/Registration ID (e.g. "CS2026-042")
    private String department;
    private String course;

    public Student() {
        super();
        setRole(Role.STUDENT);
    }

    public Student(int userId, String username, String email, String passwordHash, String fullName,
                   String studentId, String department, String course) {
        super(userId, username, email, passwordHash, Role.STUDENT, fullName);
        this.studentId = studentId;
        this.department = department;
        this.course = course;
    }

    // =========================================================================
    // POLYMORPHIC METHOD OVERRIDES
    // =========================================================================

    @Override
    public String getRoleDescription() {
        return "Enrolled Student with event registration, attendance tracking, and certificate access.";
    }

    @Override
    public String getDashboardTitle() {
        return "Student Event Portal - " + getFullName() + " (" + (studentId != null ? studentId : "N/A") + ")";
    }

    @Override
    public boolean hasAdminPrivileges() {
        return false;
    }

    // =========================================================================
    // Student Getters and Setters
    // =========================================================================

    public String getStudentId() {
        return studentId;
    }

    public void setStudentId(String studentId) {
        this.studentId = studentId;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getCourse() {
        return course;
    }

    public void setCourse(String course) {
        this.course = course;
    }
}
