package com.cmpe343.model;

/**
 * Model class representing a credit card for payment processing.
 * 
 * @author Group07
 * @version 1.0
 */
public class CreditCard {
    private String holderName;
    private String cardNumber;
    private String expiry;
    private String cvc;
    
    public CreditCard(String holderName, String cardNumber, String expiry, String cvc) {
        this.holderName = holderName;
        this.cardNumber = cardNumber;
        this.expiry = expiry;
        this.cvc = cvc;
    }
    
    public String getHolderName() {
        return holderName;
    }
    
    public String getCardNumber() {
        return cardNumber;
    }
    
    public String getExpiry() {
        return expiry;
    }
    
    public String getCvc() {
        return cvc;
    }
    
    /**
     * Validates the credit card details.
     * 
     * @return true if all fields are valid, false otherwise
     */
    public boolean isValid() {
        // Validate holder name (not empty)
        if (holderName == null || holderName.trim().isEmpty()) {
            return false;
        }
        
        // Validate card number (8 digits)
        if (cardNumber == null || !cardNumber.matches("\\d{8}")) {
            return false;
        }
        
        // Validate expiry (MM/YY format)
        if (expiry == null || !expiry.matches("\\d{2}/\\d{2}")) {
            return false;
        }
        
        // Validate CVC (3 digits)
        if (cvc == null || !cvc.matches("\\d{3}")) {
            return false;
        }
        
        return true;
    }
}
