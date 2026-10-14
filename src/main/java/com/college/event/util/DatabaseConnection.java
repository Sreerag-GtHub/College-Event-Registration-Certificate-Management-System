package com.college.event.util;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.Properties;

public class DatabaseConnection {

    private static String dbUrl;
    private static String dbUser;
    private static String dbPassword;
    private static String dbDriver = "com.mysql.cj.jdbc.Driver";
    private static boolean initialized = false;

    static {
        loadConfiguration();
    }

    public static synchronized void loadConfiguration() {
        Properties props = new Properties();

        File configFile = new File("config/database.properties");
        if (!configFile.exists()) {
            configFile = new File("../config/database.properties");
        }

        if (configFile.exists()) {
            try (InputStream in = new FileInputStream(configFile)) {
                props.load(in);
            } catch (Exception e) {
                System.err.println("Could not read config/database.properties: " + e.getMessage());
            }
        }

        dbUrl = System.getenv("DB_URL");
        if (dbUrl == null || dbUrl.trim().isEmpty()) {
            dbUrl = props.getProperty(
                    "db.url",
                    "jdbc:mysql://localhost:3306/college_event_management?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
            );
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
            System.err.println("MySQL JDBC driver not found: " + dbDriver);
        }
    }

    public static Connection getConnection() throws SQLException {
        if (!initialized) {
            loadConfiguration();
        }
        try {
            return DriverManager.getConnection(dbUrl, dbUser, dbPassword);
        } catch (SQLException e) {
            throw new SQLException(
                    "Failed to connect to MySQL at '" + getSanitizedUrl() + "' as '" + dbUser + "'.",
                    e
            );
        }
    }

    public static boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }

    public static String getSanitizedUrl() {
        if (dbUrl == null) {
            return "N/A";
        }
        int qIdx = dbUrl.indexOf('?');
        return qIdx != -1 ? dbUrl.substring(0, qIdx) : dbUrl;
    }

    public static String getDbUser() {
        return dbUser;
    }
}
