package com.college.event.model;

public class Student extends User {

    private String studentId;
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

    @Override
    public String getRoleDescription() {
        return "Student - register for events and download certificates";
    }

    @Override
    public String getDashboardTitle() {
        return "Student Dashboard - " + getFullName() + " (" + (studentId != null ? studentId : "N/A") + ")";
    }

    @Override
    public boolean hasAdminPrivileges() {
        return false;
    }

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
