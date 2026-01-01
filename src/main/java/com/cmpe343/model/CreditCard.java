package com.cmpe343.model;


import java.time.YearMonth;


public class CreditCard {
    private String holderName;
    private String cardNumber;
    private String expiryDate; // Format: MM/YY
    private String cvc;

    public CreditCard(String holderName, String cardNumber, String expiryDate, String cvc) {
        this.holderName = holderName;
        this.cardNumber = cardNumber;
        this.expiryDate = expiryDate;
        this.cvc = cvc;
    }

    public boolean isValid() {
        return isCardNumberValid() && isCvcValid() && isExpiryDateValid() && isHolderNameValid();
    }

    private boolean isHolderNameValid() {
        return holderName != null && !holderName.trim().isEmpty();
    }

    // Rule 1: Card number must be exactly 8 digits and numeric
    private boolean isCardNumberValid() {
        return cardNumber != null && cardNumber.matches("\\d{8}");
    }

    // Rule 2: CVC must be exactly 3 digits and numeric
    private boolean isCvcValid() {
        return cvc != null && cvc.matches("\\d{3}");
    }

    // Rule 3: Date must be MM/YY format and not in the past
    private boolean isExpiryDateValid() {
        if (expiryDate == null || !expiryDate.matches("(0[1-9]|1[0-2])/[0-9]{2}")) {
            return false;
        }

        try {
            String[] parts = expiryDate.split("/");
            int month = Integer.parseInt(parts[0]);
            int year = Integer.parseInt("20" + parts[1]); // Convert YY to 20YY

            YearMonth currentMonth = YearMonth.now();
            YearMonth cardExpiry = YearMonth.of(year, month);

            // Card must not be expired
            return !cardExpiry.isBefore(currentMonth);
        } catch (Exception e) {
            return false;
        }
    }

    // Getters
    public String getHolderName() { return holderName; }
    public String getCardNumber() { return cardNumber; }
    public String getExpiryDate() { return expiryDate; }
    public String getCvc() { return cvc; }
}