package com.college.event.model;

import java.sql.Timestamp;

public class Event {
    private int eventId;
    private String title;
    private String description;
    private String category;
    private String venue;
    private Timestamp eventDate;
    private Timestamp registrationDeadline;
    private int maxCapacity;
    private String status;
    private boolean requiresApproval;
    private boolean certificateEligible;
    private Integer createdBy;
    private Timestamp createdAt;

    private int registeredCount;
    private int approvedCount;

    public Event() {
        this.status = "OPEN";
        this.maxCapacity = 50;
        this.requiresApproval = true;
        this.certificateEligible = true;
    }

    public Event(int eventId, String title, String description, String category, String venue,
                 Timestamp eventDate, Timestamp registrationDeadline, int maxCapacity, String status,
                 boolean requiresApproval, boolean certificateEligible) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.category = category;
        this.venue = venue;
        this.eventDate = eventDate;
        this.registrationDeadline = registrationDeadline;
        this.maxCapacity = maxCapacity;
        this.status = status;
        this.requiresApproval = requiresApproval;
        this.certificateEligible = certificateEligible;
    }

    public boolean isDeadlinePassed() {
        if (registrationDeadline == null) return false;
        return System.currentTimeMillis() > registrationDeadline.getTime();
    }

    public boolean isAcceptingRegistrations() {
        if (!"OPEN".equalsIgnoreCase(status)) return false;
        if (isDeadlinePassed()) return false;
        if (maxCapacity > 0 && approvedCount >= maxCapacity) return false;
        return true;
    }

    public int getAvailableSeats() {
        int remaining = maxCapacity - approvedCount;
        return Math.max(0, remaining);
    }

    public int getEventId() {
        return eventId;
    }

    public void setEventId(int eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getVenue() {
        return venue;
    }

    public void setVenue(String venue) {
        this.venue = venue;
    }

    public Timestamp getEventDate() {
        return eventDate;
    }

    public void setEventDate(Timestamp eventDate) {
        this.eventDate = eventDate;
    }

    public Timestamp getRegistrationDeadline() {
        return registrationDeadline;
    }

    public void setRegistrationDeadline(Timestamp registrationDeadline) {
        this.registrationDeadline = registrationDeadline;
    }

    public int getMaxCapacity() {
        return maxCapacity;
    }

    public void setMaxCapacity(int maxCapacity) {
        this.maxCapacity = maxCapacity;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public boolean isRequiresApproval() {
        return requiresApproval;
    }

    public void setRequiresApproval(boolean requiresApproval) {
        this.requiresApproval = requiresApproval;
    }

    public boolean isCertificateEligible() {
        return certificateEligible;
    }

    public void setCertificateEligible(boolean certificateEligible) {
        this.certificateEligible = certificateEligible;
    }

    public Integer getCreatedBy() {
        return createdBy;
    }

    public void setCreatedBy(Integer createdBy) {
        this.createdBy = createdBy;
    }

    public Timestamp getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Timestamp createdAt) {
        this.createdAt = createdAt;
    }

    public int getRegisteredCount() {
        return registeredCount;
    }

    public void setRegisteredCount(int registeredCount) {
        this.registeredCount = registeredCount;
    }

    public int getApprovedCount() {
        return approvedCount;
    }

    public void setApprovedCount(int approvedCount) {
        this.approvedCount = approvedCount;
    }

    @Override
    public String toString() {
        return title + " (" + category + " - " + venue + ")";
    }
}
