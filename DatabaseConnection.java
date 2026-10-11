package com.college.event.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

/**
 * Utility class for establishing and managing JDBC database connections to MySQL.
 * 
 * Demonstrates:
 * - Centralized resource management
 * - Separation of configuration from source code (reads config/database.properties)
 * - Safe error reporting without leaking database passwords
 */
public class DatabaseConnection {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String dbDriver = "com.mysql.cj.jdbc.Driver";
    private static boolean initialized = false;

    // Static block to initialize configuration once
    static {
        loadConfiguration();
    }

    /**
     * Loads database configuration from config/database.properties or system environment variables.
     */
    public static synchronized void loadConfiguration() {
        Properties props = new Properties();

        // 1. Try loading from config/database.properties file
        File configFile = new File("config/database.properties");
        if (!configFile.exists()) {
            // Check current working directory or dist directory
            configFile = new File("../config/database.properties");
        }

        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("[DatabaseConnection] Warning: Could not read configuration file: " + e.getMessage());
            }
        }

        // 2. Read properties with fallback to Environment Variables and standard defaults
        dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            dbUrl = props.getProperty("db.url", "jdbc:mysql://localhost:3306/college_event_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC");
        }

        dbUser = System.getenv("DB_USERNAME");
        if (dbUser == null || dbUser.trim().isEmpty()) {
            dbUser = props.getProperty("db.username", "root");
        }

        dbPassword = System.getenv("DB_PASSWORD");
        if (dbPassword == null) {
            dbPassword = props.getProperty("db.password", "");
        }

        String driverProp = props.getProperty("db.driver");
        if (driverProp != null && !driverProp.trim().isEmpty()) {
            dbDriver = driverProp.trim();
        }

        try {
            Class.forName(dbDriver);
            initialized = true;
        } catch (ClassNotFoundException e) {
            System.err.println("[DatabaseConnection] Error: MySQL JDBC Driver not found on classpath: " + dbDriver);
        }
    }

    /**
     * Creates and returns a new JDBC Connection.
     * Callers should use try-with-resources to ensure connections are closed cleanly.
     *
     * @return Connection active JDBC connection to MySQL
     * @throws SQLException if connection fails
     */
    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            // Log helpful message without exposing password credentials
            String safeMsg = "Failed to connect to MySQL database at '" + getSanitizedUrl() 
                           + "' with user '" + dbUser + "'. "
                           + "Ensure MySQL service is running and credentials in config/database.properties are correct.";
            throw new SQLException(safeMsg, e);
        }
    }

    /**
     * Quick test method to verify database connectivity.
     */
    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    /**
     * Returns a sanitized string of the connection URL (hides credentials if present).
     */
    public static String getSanitizedUrl() {
        if (dbUrl == null) return "N/A";
        // Strip out any sensitive tokens from URL if present
        int qIdx = dbUrl.indexOf('?');
        return qIdx != -1 ? dbUrl.substring(0, qIdx) : dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }
}
