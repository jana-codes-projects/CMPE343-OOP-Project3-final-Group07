package com.example.demo.utils;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

/**
 * Utility class for hashing passwords using SHA-256.
 * 
 * @author Group07
 * @version 1.0
 */
public class PasswordHasher {
    /**
     * Hashes a password using SHA-256 algorithm.
     * 
     * @param password the plain text password
     * @return the hashed password as a hex string
     */
    public static String hashPassword(String password) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] hashBytes = md.digest(password.getBytes());
            return bytesToHex(hashBytes);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("SHA-256 algorithm not found", e);
        }
    }

    /**
     * Converts a byte array to hexadecimal string.
     * 
     * @param bytes the byte array
     * @return the hexadecimal string
     */
    private static String bytesToHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder();
        for (byte b : bytes) {
            sb.append(String.format("%02x", b));
        }
        return sb.toString();
    }

    /**
     * Verifies a password against a hash.
     * 
     * @param password the plain text password
     * @param hash the password hash
     * @return true if the password matches the hash
     */
    public static boolean verifyPassword(String password, String hash) {
        String hashedPassword = hashPassword(password);
        return hashedPassword.equalsIgnoreCase(hash);
    }
}

