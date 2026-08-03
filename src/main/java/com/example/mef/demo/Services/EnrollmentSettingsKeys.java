package com.example.mef.demo.Services;

/**
 * Setting keys shared between the Settings screen ({@code SettingsView})
 * and the enrollment wizard ({@code EnrollmentWizard}), kept in one place
 * so the two never drift apart.
 */
public final class EnrollmentSettingsKeys {

    private EnrollmentSettingsKeys() {
    }

    /** Minimum age (in whole years) a child must have to be enrolled. 0 = no restriction. */
    public static final String MIN_AGE = "enrollment.min_age";
    public static final int MIN_AGE_DEFAULT = 0;
}