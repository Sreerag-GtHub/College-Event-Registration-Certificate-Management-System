package com.college.event;

import com.college.event.ui.LoginFrame;
import com.college.event.util.DatabaseConnection;

import javax.swing.*;
import java.sql.Connection;

public class Main {

    public static void main(String[] args) {
        try {
            UIManager.setLookAndFeel("com.formdev.flatlaf.FlatLightLaf");
        } catch (Exception e) {
            try {
                UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
            } catch (Exception ignored) {
            }
        }

        new Thread(() -> {
            try (Connection conn = DatabaseConnection.getConnection()) {
                System.out.println("Database connected.");
            } catch (Exception e) {
                System.err.println("Database connection failed: " + e.getMessage());
                SwingUtilities.invokeLater(() -> JOptionPane.showMessageDialog(
                        null,
                        "Could not connect to MySQL.\n\n"
                                + "URL: " + DatabaseConnection.getSanitizedUrl() + "\n"
                                + "User: " + DatabaseConnection.getDbUser() + "\n\n"
                                + "1. Start MySQL\n"
                                + "2. Run database/schema.sql\n"
                                + "3. Check config/database.properties",
                        "Database Warning",
                        JOptionPane.WARNING_MESSAGE
                ));
            }
        }).start();

        SwingUtilities.invokeLater(() -> new LoginFrame().setVisible(true));
    }
}
