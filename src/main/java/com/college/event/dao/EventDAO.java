package com.college.event.dao;

import com.college.event.model.Event;
import com.college.event.util.DatabaseConnection;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class EventDAO {

    public boolean createEvent(Event event) throws SQLException {
        String sql = "INSERT INTO events (title, description, category, venue, event_date, registration_deadline, "
                   + "max_capacity, status, requires_approval, certificate_eligible, created_by) "
                   + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {

            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getCategory());
            stmt.setString(4, event.getVenue());
            stmt.setTimestamp(5, event.getEventDate());
            stmt.setTimestamp(6, event.getRegistrationDeadline());
            stmt.setInt(7, event.getMaxCapacity());
            stmt.setString(8, event.getStatus());
            stmt.setBoolean(9, event.isRequiresApproval());
            stmt.setBoolean(10, event.isCertificateEligible());
            if (event.getCreatedBy() != null) {
                stmt.setInt(11, event.getCreatedBy());
            } else {
                stmt.setNull(11, Types.INTEGER);
            }

            int rows = stmt.executeUpdate();
            if (rows > 0) {
                try (ResultSet keys = stmt.getGeneratedKeys()) {
                    if (keys.next()) {
                        event.setEventId(keys.getInt(1));
                    }
                }
                return true;
            }
            return false;
        }
    }

    public boolean updateEvent(Event event) throws SQLException {
        String sql = "UPDATE events SET title = ?, description = ?, category = ?, venue = ?, "
                   + "event_date = ?, registration_deadline = ?, max_capacity = ?, status = ?, "
                   + "requires_approval = ?, certificate_eligible = ? WHERE event_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setString(1, event.getTitle());
            stmt.setString(2, event.getDescription());
            stmt.setString(3, event.getCategory());
            stmt.setString(4, event.getVenue());
            stmt.setTimestamp(5, event.getEventDate());
            stmt.setTimestamp(6, event.getRegistrationDeadline());
            stmt.setInt(7, event.getMaxCapacity());
            stmt.setString(8, event.getStatus());
            stmt.setBoolean(9, event.isRequiresApproval());
            stmt.setBoolean(10, event.isCertificateEligible());
            stmt.setInt(11, event.getEventId());

            return stmt.executeUpdate() > 0;
        }
    }

    public boolean updateStatus(int eventId, String newStatus) throws SQLException {
        String sql = "UPDATE events SET status = ? WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setString(1, newStatus);
            stmt.setInt(2, eventId);
            return stmt.executeUpdate() > 0;
        }
    }

    public boolean deleteEvent(int eventId) throws SQLException {
        String sql = "DELETE FROM events WHERE event_id = ?";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            stmt.setInt(1, eventId);
            return stmt.executeUpdate() > 0;
        }
    }

    public Event findById(int eventId) throws SQLException {
        String sql = "SELECT e.*, "
                   + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.event_id) AS reg_count, "
                   + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.event_id AND r.status = 'APPROVED') AS approved_count "
                   + "FROM events e WHERE e.event_id = ?";

        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {

            stmt.setInt(1, eventId);
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapRowToEvent(rs);
                }
            }
        }
        return null;
    }

    public List<Event> findAllEvents() throws SQLException {
        String sql = "SELECT e.*, "
                   + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.event_id) AS reg_count, "
                   + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.event_id AND r.status = 'APPROVED') AS approved_count "
                   + "FROM events e ORDER BY e.event_date ASC";

        List<Event> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {
                list.add(mapRowToEvent(rs));
            }
        }
        return list;
    }

    public List<Event> searchAndFilter(String keyword, String category, boolean onlyOpen) throws SQLException {
        StringBuilder sql = new StringBuilder(
            "SELECT e.*, "
          + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.event_id) AS reg_count, "
          + "(SELECT COUNT(*) FROM registrations r WHERE r.event_id = e.event_id AND r.status = 'APPROVED') AS approved_count "
          + "FROM events e WHERE 1=1 "
        );

        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.trim().isEmpty()) {
            sql.append("AND (e.title LIKE ? OR e.description LIKE ? OR e.venue LIKE ?) ");
            String term = "%" + keyword.trim() + "%";
            params.add(term);
            params.add(term);
            params.add(term);
        }

        if (category != null && !category.trim().isEmpty() && !"All Categories".equalsIgnoreCase(category)) {
            sql.append("AND e.category = ? ");
            params.add(category.trim());
        }

        if (onlyOpen) {
            sql.append("AND e.status = 'OPEN' AND e.registration_deadline > CURRENT_TIMESTAMP ");
        }

        sql.append("ORDER BY e.event_date ASC");

        List<Event> list = new ArrayList<>();
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {

            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }

            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    list.add(mapRowToEvent(rs));
                }
            }
        }
        return list;
    }

    public int countEvents() throws SQLException {
        String sql = "SELECT COUNT(*) FROM events";
        try (Connection conn = DatabaseConnection.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    private Event mapRowToEvent(ResultSet rs) throws SQLException {
        Event e = new Event();
        e.setEventId(rs.getInt("event_id"));
        e.setTitle(rs.getString("title"));
        e.setDescription(rs.getString("description"));
        e.setCategory(rs.getString("category"));
        e.setVenue(rs.getString("venue"));
        e.setEventDate(rs.getTimestamp("event_date"));
        e.setRegistrationDeadline(rs.getTimestamp("registration_deadline"));
        e.setMaxCapacity(rs.getInt("max_capacity"));
        e.setStatus(rs.getString("status"));
        e.setRequiresApproval(rs.getBoolean("requires_approval"));
        e.setCertificateEligible(rs.getBoolean("certificate_eligible"));

        int createdByVal = rs.getInt("created_by");
        if (!rs.wasNull()) {
            e.setCreatedBy(createdByVal);
        }
        e.setCreatedAt(rs.getTimestamp("created_at"));

        try {
            e.setRegisteredCount(rs.getInt("reg_count"));
            e.setApprovedCount(rs.getInt("approved_count"));
        } catch (SQLException ignored) {

        }

        return e;
    }
}
