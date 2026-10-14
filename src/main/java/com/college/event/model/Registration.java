package com.college.event.model;

import java.sql.Timestamp;

public class Registration {
    private int registrationId;
    private int eventId;
    private int studentId;
    private Timestamp registrationDate;
    private String status;
    private String remarks;

    private String eventTitle;
    private Timestamp eventDate;
    private String studentName;
    private String studentRollNo;
    private String department;
    private String attendanceStatus;
    private boolean certificateIssued;
    private String certificateCode;

    public Registration() {
        this.status = "PENDING";
        this.attendanceStatus = "NOT_RECORDED";
    }

    public Registration(int registrationId, int eventId, int studentId, Timestamp registrationDate, String status) {
        this.registrationId = registrationId;
        this.eventId = eventId;
        this.studentId = studentId;
        this.registrationDate = registrationDate;
        this.status = status;
        this.attendanceStatus = "NOT_RECORDED";
    }

    public int getRegistrationId() {
        return registrationId;
    }

    public void setRegistrationId(int registrationId) {
        this.registrationId = registrationId;
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public int getStudentId() {
        return studentId;
    }

    public void setStudentId(int studentId) {
        this.studentId = studentId;
    }

    public Timestamp getRegistrationDate() {
        return registrationDate;
    }

    public void setRegistrationDate(Timestamp registrationDate) {
        this.registrationDate = registrationDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getRemarks() {
        return remarks;
    }

    public void setRemarks(String remarks) {
        this.remarks = remarks;
    }

    public String getEventTitle() {
        return eventTitle;
    }

    public void setEventTitle(String eventTitle) {
        this.eventTitle = eventTitle;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public String getStudentName() {
        return studentName;
    }

    public void setStudentName(String studentName) {
        this.studentName = studentName;
    }

    public String getStudentRollNo() {
        return studentRollNo;
    }

    public void setStudentRollNo(String studentRollNo) {
        this.studentRollNo = studentRollNo;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getAttendanceStatus() {
        return attendanceStatus != null ? attendanceStatus : "NOT_RECORDED";
    }

    public void setAttendanceStatus(String attendanceStatus) {
        this.attendanceStatus = attendanceStatus;
    }

    public boolean isCertificateIssued() {
        return certificateIssued;
    }

    public void setCertificateIssued(boolean certificateIssued) {
        this.certificateIssued = certificateIssued;
    }

    public String getCertificateCode() {
        return certificateCode;
    }

    public void setCertificateCode(String certificateCode) {
        this.certificateCode = certificateCode;
    }
}
