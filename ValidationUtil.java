package com.college.event.util;

import java.sql.Timestamp;
import java.util.regex.Pattern;

/**
 * Utility class for validating user input, form data, and business constraints.
 */
public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    /**
     * Validates whether a string is non-null and not empty/whitespace.
     */
    public static boolean isNonEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    /**
     * Validates email format according to standard email RFC regex.
     */
    public static boolean isValidEmail(String email) {
        if (!isNonEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    /**
     * Checks if password meets basic length requirements (minimum 6 characters).
     */
    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * Verifies if two password fields match.
     */
    public static boolean passwordsMatch(String p1, String p2) {
        if (p1 == null || p2 == null) return false;
        return p1.equals(p2);
    }

    /**
     * Validates that an integer string represents a strictly positive value (> 0).
     */
    public static boolean isPositiveInteger(String numStr) {
        if (!isNonEmpty(numStr)) return false;
        try {
            int val = Integer.parseInt(numStr.trim());
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Ensures that the registration deadline occurs before the actual event date.
     */
    public static boolean isDeadlineBeforeEventDate(Timestamp deadline, Timestamp eventDate) {
        if (deadline == null || eventDate == null) return false;
        return deadline.before(eventDate);
    }

    /**
     * Trims and returns non-null string.
     */
    public static String clean(String input) {
        return input == null ? "" : input.trim();
    }
}
