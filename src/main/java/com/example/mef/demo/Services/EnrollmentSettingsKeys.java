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

    // ── Per-category age rules ──────────────────────────────────────────────
    // Each category has a minimum and a maximum age (in complete years).
    // A value of 0 for min means no lower bound; a value of 0 for max means no upper bound.

    /** حضانة (Crèche) age rules: child must be >= minAge and <= maxAge (0 = unrestricted). */
    public static final String CRECHE_MIN_AGE = "enrollment.category.creche.min_age";
    public static final String CRECHE_MAX_AGE = "enrollment.category.creche.max_age";
    public static final int CRECHE_MIN_AGE_DEFAULT = 2;  // au moins 2 ans
    public static final int CRECHE_MAX_AGE_DEFAULT = 0;  // pas de limite superieure

    /** تحضير (Préparatoire) age rules: child must be < maxAge. */
    public static final String PREPARATOIRE_MIN_AGE = "enrollment.category.preparatoire.min_age";
    public static final String PREPARATOIRE_MAX_AGE = "enrollment.category.preparatoire.max_age";
    public static final int PREPARATOIRE_MIN_AGE_DEFAULT = 0;  // pas de limite inferieure
    public static final int PREPARATOIRE_MAX_AGE_DEFAULT = 5;  // moins de 5 ans (exclusif)

    /** دعم (Soutien) age rules: child must be > minAge. */
    public static final String SOUTIEN_MIN_AGE = "enrollment.category.soutien.min_age";
    public static final String SOUTIEN_MAX_AGE = "enrollment.category.soutien.max_age";
    public static final int SOUTIEN_MIN_AGE_DEFAULT = 6;  // plus de 6 ans (exclusif)
    public static final int SOUTIEN_MAX_AGE_DEFAULT = 0;  // pas de limite superieure

    /** Fee used to calculate a crèche child's monthly due depending on plan/session. */
    public static final String CRECHE_HALF_DAY_FEE = "enrollment.creche_half_day_fee";
    public static final String CRECHE_HALF_DAY_LUNCH_FEE = "enrollment.creche_half_day_lunch_fee";
    public static final String CRECHE_FULL_DAY_FEE = "enrollment.creche_full_day_fee";
    public static final String CRECHE_DAY_BY_DAY_FEE = "enrollment.creche_day_by_day_fee";
    public static final double CRECHE_FEE_DEFAULT = 0d;

    /** Day of month when crèche dues become payable (normally the first day). */
    public static final String CRECHE_DUE_DAY = "enrollment.creche_due_day";
    public static final int CRECHE_DUE_DAY_DEFAULT = 1;
}
