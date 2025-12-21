package com.cmpe343.fx;

import com.cmpe343.model.User;

public final class Session {
    private static User currentUser;

    private Session() {
    }

    public static void setUser(User u) {
        currentUser = u;
    }

    public static User getUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void clear() {
        currentUser = null;
    }
}
