package com.example.mef.demo.config;


import com.example.mef.demo.Model.User;

import java.time.Duration;
import java.time.Instant;
import java.util.OptionalInt;
import java.util.prefs.Preferences;

/** Holds the currently authenticated user for the lifetime of the app run. */
public final class Session {

    private static final long REMEMBER_DURATION_MILLIS = Duration.ofMinutes(10).toMillis();
    private static final String USER_ID_KEY = "rememberedUserId";
    private static final String EXPIRES_AT_KEY = "rememberedSessionExpiresAt";
    private static final Preferences PREFERENCES = Preferences.userNodeForPackage(Session.class);

    private static User currentUser;

    private Session() {}

    public static void login(User user) {
        currentUser = user;
        PREFERENCES.putInt(USER_ID_KEY, user.getId());
        PREFERENCES.putLong(EXPIRES_AT_KEY, Instant.now().toEpochMilli() + REMEMBER_DURATION_MILLIS);
    }

    /** Restores a previously verified user without extending the original 10-minute timeout. */
    public static void restore(User user) {
        currentUser = user;
    }

    public static void logout() {
        currentUser = null;
        PREFERENCES.remove(USER_ID_KEY);
        PREFERENCES.remove(EXPIRES_AT_KEY);
    }

    /** Returns the remembered user only while the local 10-minute session has not expired. */
    public static OptionalInt rememberedUserId() {
        long expiresAt = PREFERENCES.getLong(EXPIRES_AT_KEY, 0);
        int userId = PREFERENCES.getInt(USER_ID_KEY, -1);
        if (userId < 0 || expiresAt <= Instant.now().toEpochMilli()) {
            logout();
            return OptionalInt.empty();
        }
        return OptionalInt.of(userId);
    }

    public static User getCurrentUser() {
        return currentUser;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}
