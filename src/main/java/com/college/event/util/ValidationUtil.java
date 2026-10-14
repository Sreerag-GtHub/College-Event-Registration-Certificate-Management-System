package com.college.event.util;

import java.sql.Timestamp;
import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN = Pattern.compile(
        "^[a-zA-Z0-9_+&*-]+(?:\\.[a-zA-Z0-9_+&*-]+)*@(?:[a-zA-Z0-9-]+\\.)+[a-zA-Z]{2,7}$"
    );

    public static boolean isNonEmpty(String text) {
        return text != null && !text.trim().isEmpty();
    }

    public static boolean isValidEmail(String email) {
        if (!isNonEmpty(email)) return false;
        return EMAIL_PATTERN.matcher(email.trim()).matches();
    }

    public static boolean isValidPassword(String password) {
        return password != null && password.length() >= 6;
    }

    public static boolean passwordsMatch(String p1, String p2) {
        if (p1 == null || p2 == null) return false;
        return p1.equals(p2);
    }

    public static boolean isPositiveInteger(String numStr) {
        if (!isNonEmpty(numStr)) return false;
        try {
            int val = Integer.parseInt(numStr.trim());
            return val > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    public static boolean isDeadlineBeforeEventDate(Timestamp deadline, Timestamp eventDate) {
        if (deadline == null || eventDate == null) return false;
        return deadline.before(eventDate);
    }

    public static String clean(String input) {
        return input == null ? "" : input.trim();
    }
}
