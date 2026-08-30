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

    /** Daily fee used to calculate a crèche child's monthly due from attendance days. */
    public static final String CRECHE_DAILY_FEE = "enrollment.creche_daily_fee";
    public static final double CRECHE_DAILY_FEE_DEFAULT = 0d;

    /** Day of month when crèche dues become payable (normally the first day). */
    public static final String CRECHE_DUE_DAY = "enrollment.creche_due_day";
    public static final int CRECHE_DUE_DAY_DEFAULT = 1;
}
