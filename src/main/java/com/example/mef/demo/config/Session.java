package com.example.mef.demo.config;


import com.example.mef.demo.Model.User;

/** Holds the currently authenticated user for the lifetime of the app run. */
public final class Session {

    private static User currentUser;

    private Session() {}

    public static void login(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
