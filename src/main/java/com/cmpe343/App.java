package com.cmpe343;

import com.cmpe343.app.Menu;
import com.cmpe343.model.User;
import com.cmpe343.service.AuthService;

import java.util.Scanner;

public class App {

    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        AuthService auth = new AuthService();

        System.out.print("Username: ");
        String username = sc.nextLine();

        System.out.print("Password: ");
        String password = sc.nextLine();

        User user = auth.login(username, password);

        if (user == null) {
            System.out.println("❌ Invalid username or password");
        } else {
            Menu.show(user);
        }
        
        sc.close();
    }
}
