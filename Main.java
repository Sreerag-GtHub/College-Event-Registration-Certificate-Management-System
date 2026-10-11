package com.college.event;

import com.college.event.ui.LoginFrame;
import com.college.event.util.DatabaseConnection;

import javax.swing.*;
import java.sql.Connection;

/**
 * Main application entry point for College Event Registration & Certificate Management System.
 * 
 * Desktop Native Application using Java 21, Swing, and JDBC with MySQL.
 */
public class Main {

    public static void main(String[] args) {
        // 1. Initialize modern Look and Feel (FlatLaf with fallback to System L&F)
        initializeLookAndFeel();

        // 2. Perform initial non-blocking database connectivity check
        checkDatabaseConnectivityAsync();

        // 3. Start Swing interface on Event Dispatch Thread (EDT)
        SwingUtilities.invokeLater(() -> {
            LoginFrame loginFrame = new LoginFrame();
            loginFrame.setVisible(true);
        });
    }

    /**
     * Configures the user interface appearance.
     * Uses FlatLaf for clean modern typography and borders, falling back to System L&F.
     */
    private static void initializeLookAndFeel() {
        try {
            // Attempt FlatLaf modern Light theme
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception e) {
            try {
                // Fallback to Native System Look & Feel
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ex) {
                System.err.println("Default look and feel used: " + ex.getMessage());
            }
        }
    }

    /**
     * Checks database connectivity in a background thread to prevent GUI startup hangs.
     */
    private static void checkDatabaseConnectivityAsync() {
        new Thread(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                System.out.println("[✓] MySQL Database connection verified successfully.");
            } catch (Exception e) {
                System.err.println("[!] Notice: Unable to connect to MySQL database at startup: " + e.getMessage());
                SwingUtilities.invokeLater(() -> {
                    JOptionPane.showMessageDialog(
                        null,
                        "MySQL Database Connection Warning:\n\n" +
                        "Could not connect to MySQL at: " + DatabaseConnection.getSanitizedUrl() + "\n" +
                        "User: " + DatabaseConnection.getDbUser() + "\n\n" +
                        "Please verify:\n" +
                        "1. MySQL Community Server service is running locally.\n" +
                        "2. Database 'college_event_management' has been created via database/schema.sql.\n" +
                        "3. Credentials in 'config/database.properties' match your local MySQL installation.\n\n" +
                        "You can update config/database.properties at any time.",
                        "Database Configuration Notice",
                        JOptionPane.WARNING_MESSAGE
                    );
                });
            }
        }).start();
    }
}
