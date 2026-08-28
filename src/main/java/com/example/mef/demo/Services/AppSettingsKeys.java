package com.example.mef.demo.Services;

/**
 * Setting keys for app-wide preferences (as opposed to per-module rules
 * like {@link EnrollmentSettingsKeys}), kept in one place so the language
 * switcher and app startup never drift apart.
 */
public final class AppSettingsKeys {

    private AppSettingsKeys() {
    }

    /** Display language for the whole app. Stored as a language tag ("fr" or "ar"). */
    public static final String LOCALE = "app.locale";
    public static final String LOCALE_DEFAULT = "fr";

    /** Name displayed for the school/centre throughout the desktop application. */
    public static final String SCHOOL_NAME = "app.school_name";
    public static final String SCHOOL_NAME_DEFAULT = "Rawdati";
}
