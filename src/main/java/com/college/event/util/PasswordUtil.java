package com.college.event.util;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;

public class PasswordUtil {

    private static final SecureRandom RANDOM = new SecureRandom();

    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be empty.");
        }

        byte[] salt = new byte[16];
        RANDOM.nextBytes(salt);

        String hash = sha256(password, salt);
        String saltText = Base64.getEncoder().encodeToString(salt);

        return "SHA256$" + saltText + "$" + hash;
    }

    public static boolean verifyPassword(String password, String storedHash) {
        if (password == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        String[] parts = storedHash.split("\\$");
        if (parts.length != 3 || !"SHA256".equals(parts[0])) {
            return false;
        }

        try {
            byte[] salt = Base64.getDecoder().decode(parts[1]);
            String expected = parts[2];
            String actual = sha256(password, salt);
            return expected.equals(actual);
        } catch (Exception e) {
            return false;
        }
    }

    private static String sha256(String password, byte[] salt) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            digest.update(salt);
            byte[] hash = digest.digest(password.getBytes(StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (Exception e) {
            throw new RuntimeException("Could not hash password.", e);
        }
    }
}
