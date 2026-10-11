package com.college.event.util;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.util.Base64;

/**
 * Utility for cryptographic password hashing and verification.
 * 
 * Implements:
 * - PBKDF2 with HMAC-SHA256 (NIST recommended, salted, iterated 65,536 times)
 * - Cryptographically secure pseudo-random salt via SecureRandom
 * - Constant-time comparison to prevent timing attacks
 * - Backward compatibility with BCrypt (via jBCrypt) if BCrypt hashes are provided
 */
public class PasswordUtil {

    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 65536;
    private static final int KEY_LENGTH = 256; // bits
    private static final int SALT_BYTES = 16;  // 128 bits

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Hashes a plaintext password using PBKDF2 with a fresh random salt.
     * Formats result as "PBKDF2$<iterations>$<base64Salt>$<base64Hash>"
     *
     * @param password Plaintext password to hash
     * @return Formatted hash string suitable for database storage
     */
    public static String hashPassword(String password) {
        if (password == null || password.isEmpty()) {
            throw new IllegalArgumentException("Password cannot be null or empty.");
        }

        byte[] salt = new byte[SALT_BYTES];
        RANDOM.nextBytes(salt);

        byte[] hash = pbkdf2(password.toCharArray(), salt, ITERATIONS, KEY_LENGTH);

        String saltBase64 = Base64.getEncoder().encodeToString(salt);
        String hashBase64 = Base64.getEncoder().encodeToString(hash);

        return "PBKDF2$" + ITERATIONS + "$" + saltBase64 + "$" + hashBase64;
    }

    /**
     * Verifies whether a candidate plaintext password matches the stored hash string.
     *
     * @param candidate  Plaintext password provided during login
     * @param storedHash Hash string retrieved from MySQL database
     * @return true if password matches, false otherwise
     */
    public static boolean verifyPassword(String candidate, String storedHash) {
        if (candidate == null || storedHash == null || storedHash.isEmpty()) {
            return false;
        }

        // 1. Verify PBKDF2 format
        if (storedHash.startsWith("PBKDF2$")) {
            String[] parts = storedHash.split("\\$");
            if (parts.length != 4) {
                return false;
            }

            try {
                int iterations = Integer.parseInt(parts[1]);
                byte[] salt = Base64.getDecoder().decode(parts[2]);
                byte[] expectedHash = Base64.getDecoder().decode(parts[3]);

                byte[] candidateHash = pbkdf2(candidate.toCharArray(), salt, iterations, expectedHash.length * 8);

                return slowEquals(candidateHash, expectedHash);
            } catch (Exception e) {
                return false;
            }
        }

        // 2. Verify BCrypt format ($2a$, $2b$, $2y$) if present
        if (storedHash.startsWith("$2a$") || storedHash.startsWith("$2b$") || storedHash.startsWith("$2y$")) {
            try {
                return org.mindrot.jbcrypt.BCrypt.checkpw(candidate, storedHash);
            } catch (Exception e) {
                return false;
            }
        }

        return false;
    }

    /**
     * Executes the PBKDF2 key derivation.
     */
    private static byte[] pbkdf2(char[] password, byte[] salt, int iterations, int keyLengthBits) {
        try {
            PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, keyLengthBits);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM);
            return skf.generateSecret(spec).getEncoded();
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new RuntimeException("Error computing PBKDF2 password hash: " + e.getMessage(), e);
        }
    }

    /**
     * Constant-time byte comparison to protect against side-channel timing attacks.
     */
    private static boolean slowEquals(byte[] a, byte[] b) {
        if (a == null || b == null) return false;
        int diff = a.length ^ b.length;
        for (int i = 0; i < a.length && i < b.length; i++) {
            diff |= a[i] ^ b[i];
        }
        return diff == 0;
    }
}
