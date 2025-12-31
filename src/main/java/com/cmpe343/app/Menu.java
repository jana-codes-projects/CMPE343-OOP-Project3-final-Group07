package com.cmpe343.app;

import com.cmpe343.model.User;

public class Menu {

    public static void show(User user) {
        System.out.println("\nWelcome, " + user.getUsername() + "!");
        System.out.println("Your role: " + user.getRole());
        

        switch (user.getRole()) {
            case "customer" -> customerMenu();
            case "carrier" -> carrierMenu();
            case "owner" -> ownerMenu();
            default -> System.out.println("Unknown role.");
        }
    }

    private static void customerMenu() {
        System.out.println("Customer Menu:");
        System.out.println("1. View Products");
        System.out.println("2. Place Order");
        System.out.println("3. My Orders");
    }

    private static void carrierMenu() {
        System.out.println("Carrier Menu:");
        System.out.println("1. View Deliveries");
        System.out.println("2. Deliver the order");
    }

    private static void ownerMenu() {
        System.out.println("Owner Menu:");
        System.out.println("1. Product Management");
        System.out.println("2. View Orders");
        System.out.println("3. Reports");
    }

}