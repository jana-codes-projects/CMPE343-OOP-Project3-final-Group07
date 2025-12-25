package com.example.demo.utils;

import java.math.BigDecimal;

/**
 * Utility class for input validation.
 * Provides methods to validate various user inputs according to project requirements.
 * 
 * @author Group07
 * @version 1.0
 */
public class InputValidator {
    
    /**
     * Validates that a product quantity is a positive number.
     * 
     * @param input the input string
     * @return true if valid (positive number)
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
     * Parses and validates a quantity string, returning the BigDecimal value.
     * 
     * @param input the input string
     * @return the parsed BigDecimal value, or null if invalid
     */
    public static BigDecimal parseQuantity(String input) {
        try {
            double value = Double.parseDouble(input);
            if (value > 0) {
                return BigDecimal.valueOf(value);
            }
        } catch (NumberFormatException e) {
            // Invalid format
        }
        return null;
    }

    /**
     * Validates that a threshold value is positive.
     * 
     * @param value the threshold value
     * @return true if valid (positive)
     */
    public static boolean isValidThreshold(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }

    /**
     * Validates that a price is positive.
     * 
     * @param value the price value
     * @return true if valid (positive)
     */
    public static boolean isValidPrice(BigDecimal value) {
        return value != null && value.compareTo(BigDecimal.ZERO) > 0;
    }
}

