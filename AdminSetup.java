package com.college.event.util;

import com.college.event.model.Admin;
import com.college.event.model.Role;

import java.io.Console;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.Scanner;

/**
 * Command-line administrative initialization utility.
 * 
 * Safely creates the initial Administrator account in MySQL.
 * - Checks if an administrator account already exists.
 * - Prompts for admin full name, username, email, and password.
 * - Hashes password using PBKDF2/BCrypt before inserting into database.
 * - Never logs or stores plaintext passwords.
 */
public class AdminSetup {

    public static void main(String[] args) {
        System.out.println("==================================================================");
        System.out.println("   College Event Management System - Administrator Setup Utility  ");
        System.out.println("==================================================================");

        // Verify DB connection first
        try (Connection conn = DatabaseConnection.getConnection()) {
            System.out.println("[✓] Connected to MySQL successfully.");

            // Check existing admins
            String checkSql = "SELECT COUNT(*) FROM users WHERE role = 'ADMIN'";
            try (PreparedStatement checkStmt = conn.prepareStatement(checkSql);
                 ResultSet rs = checkStmt.executeQuery()) {
                if (rs.next() && rs.getInt(1) > 0) {
                    System.out.println("[i] Notice: " + rs.getInt(1) + " administrator account(s) already exist.");
                    System.out.print("Do you wish to create an additional administrator? (y/N): ");
                    Scanner sc = new Scanner(System.in);
                    String reply = sc.nextLine().trim();
                    if (!reply.equalsIgnoreCase("y")) {
                        System.out.println("Setup aborted. Existing administrator accounts retained.");
                        return;
                    }
                }
            }

            Scanner scanner = new Scanner(System.in);

            System.out.print("Enter Administrator Full Name (e.g. Dr. Jane Doe): ");
            String fullName = scanner.nextLine().trim();
            while (fullName.isEmpty()) {
                System.out.print("Full name cannot be empty. Please re-enter: ");
                fullName = scanner.nextLine().trim();
            }

            System.out.print("Enter Admin Username (e.g. admin): ");
            String username = scanner.nextLine().trim();
            while (username.isEmpty()) {
                System.out.print("Username cannot be empty. Please re-enter: ");
                username = scanner.nextLine().trim();
            }

            System.out.print("Enter Admin Email (e.g. admin@college.edu): ");
            String email = scanner.nextLine().trim();
            while (!ValidationUtil.isValidEmail(email)) {
                System.out.print("Invalid email format. Please enter a valid email: ");
                email = scanner.nextLine().trim();
            }

            // Secure password entry
            String password = null;
            Console console = System.console();
            if (console != null) {
                char[] passChars = console.readPassword("Enter Admin Password (min 6 chars): ");
                password = new String(passChars);
            } else {
                System.out.print("Enter Admin Password (min 6 chars): ");
                password = scanner.nextLine().trim();
            }

            while (!ValidationUtil.isValidPassword(password)) {
                System.out.print("Password must be at least 6 characters. Please re-enter: ");
                if (console != null) {
                    password = new String(console.readPassword());
                } else {
                    password = scanner.nextLine().trim();
                }
            }

            // Hash the password securely
            String passwordHash = PasswordUtil.hashPassword(password);

            // Insert into MySQL users table
            String insertSql = "INSERT INTO users (username, email, password_hash, role, full_name) VALUES (?, ?, ?, 'ADMIN', ?)";
            try (PreparedStatement pstmt = conn.prepareStatement(insertSql)) {
                pstmt.setString(1, username);
                pstmt.setString(2, email);
                pstmt.setString(3, passwordHash);
                pstmt.setString(4, fullName);
                int rows = pstmt.executeUpdate();

                if (rows > 0) {
                    System.out.println("==================================================================");
                    System.out.println("[✓] Administrator account created successfully!");
                    System.out.println("    Username: " + username);
                    System.out.println("    Email:    " + email);
                    System.out.println("    Role:     ADMIN");
                    System.out.println("You can now launch the desktop application and log in.");
                    System.out.println("==================================================================");
                }
            }

        } catch (Exception e) {
            System.err.println("[✗] Error during administrator setup: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
