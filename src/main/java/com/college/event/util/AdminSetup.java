package com.college.event.util;

import com.college.event.dao.UserDAO;
import com.college.event.model.Admin;

import java.io.Console;
import java.util.Scanner;

public class AdminSetup {

    public static void main(String[] args) {
        System.out.println("==============================================");
        System.out.println(" Administrator Setup");
        System.out.println("==============================================");

        try {
            if (!DatabaseConnection.testConnection()) {
                System.err.println("Could not connect to the database.");
                return;
            }

            Scanner scanner = new Scanner(System.in);
            UserDAO userDAO = new UserDAO();

            System.out.print("Full name: ");
            String fullName = scanner.nextLine().trim();
            while (fullName.isEmpty()) {
                System.out.print("Full name cannot be empty: ");
                fullName = scanner.nextLine().trim();
            }

            System.out.print("Username: ");
            String username = scanner.nextLine().trim();
            while (username.isEmpty() || userDAO.existsByUsername(username)) {
                System.out.print("Enter a valid unused username: ");
                username = scanner.nextLine().trim();
            }

            System.out.print("Email: ");
            String email = scanner.nextLine().trim();
            while (!ValidationUtil.isValidEmail(email) || userDAO.existsByEmail(email)) {
                System.out.print("Enter a valid unused email: ");
                email = scanner.nextLine().trim();
            }

            String password;
            Console console = System.console();
            if (console != null) {
                password = new String(console.readPassword("Password (min 6 chars): "));
            } else {
                System.out.print("Password (min 6 chars): ");
                password = scanner.nextLine().trim();
            }

            while (!ValidationUtil.isValidPassword(password)) {
                System.out.print("Password must be at least 6 characters: ");
                if (console != null) {
                    password = new String(console.readPassword());
                } else {
                    password = scanner.nextLine().trim();
                }
            }

            Admin admin = new Admin();
            admin.setFullName(fullName);
            admin.setUsername(username);
            admin.setEmail(email);
            admin.setPasswordHash(PasswordUtil.hashPassword(password));

            if (userDAO.createUser(admin)) {
                System.out.println("Administrator created successfully.");
                System.out.println("Username: " + username);
            } else {
                System.err.println("Failed to create administrator.");
            }
        } catch (Exception e) {
            System.err.println("Setup failed: " + e.getMessage());
        }
    }
}
