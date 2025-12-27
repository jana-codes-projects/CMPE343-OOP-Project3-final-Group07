package com.example.demo.services;


public class ValidationService {

    /**
     * Checks if the entered amount is a valid positive double.
     * Required by: "Reject zero, negative, non-numeric" rule.
     */
    public static boolean isValidQuantity(String input) {
        try {
            double value = Double.parseDouble(input);
            return value > 0;
        } catch (NumberFormatException e) {
            return false;
        }
    }

    /**
     * Checks if a password meets the "Strong password rules".
     */
    public static boolean isStrongPassword(String password) {
        return password != null && password.length() >= 6;
    }

    /**
     * General check for non-empty fields.
     */
    public static boolean isNotEmpty(String... fields) {
        for (String field : fields) {
            if (field == null || field.trim().isEmpty()) {
                return false;
            }
        }
        return true;
    }
}