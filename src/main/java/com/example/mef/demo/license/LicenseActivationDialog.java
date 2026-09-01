package com.example.mef.demo.license;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.prefs.Preferences;

@Component
public class LicenseActivationDialog {

    private final MachineIdentifier machineIdentifier;
    private final LicenseValidator licenseValidator;
    private final SettingsRepository settingsRepository;

    private static final String LICENSE_KEY_SETTING    = "license_key";
    private static final String TRIAL_START_SETTING    = "trial_start_date";
    private static final String LAST_SEEN_DATE_SETTING = "license_last_seen_date";
    private static final int    TRIAL_DAYS             = 7;

    private final Preferences preferences = Preferences.userNodeForPackage(LicenseActivationDialog.class);

    /**
     * One-shot startup cache for {@link #isUsable()}.
     * Computed once on first call, then reused for the lifetime of the JVM session.
     * Invalidated (reset to null) when {@link #activate(String)} is called so the
     * result is refreshed after the user enters a valid key.
     */
    private Boolean cachedIsUsable = null;

    public LicenseActivationDialog(MachineIdentifier machineIdentifier,
                                   LicenseValidator licenseValidator,
                                   SettingsRepository settingsRepository) {
        this.machineIdentifier = machineIdentifier;
        this.licenseValidator = licenseValidator;
        this.settingsRepository = settingsRepository;
    }

    public boolean isAlreadyActivated() {
        if (hasClockRollback()) return false;
        String machineId = machineIdentifier.getOrCreateMachineId();
        String storedKey = settingsRepository.get(LICENSE_KEY_SETTING);
        if (storedKey != null && licenseValidator.validate(machineId, storedKey).isPresent()) {
            preferences.put(LICENSE_KEY_SETTING, storedKey);
            return true;
        }

        String profileKey = preferences.get(LICENSE_KEY_SETTING, null);
        if (profileKey != null && licenseValidator.validate(machineId, profileKey).isPresent()) {
            settingsRepository.set(LICENSE_KEY_SETTING, profileKey);
            return true;
        }
        return false;
    }

    /** Returns the valid subscription details, including plan and expiry date. */
    public Optional<LicensePayload> getActiveLicense() {
        if (hasClockRollback()) return Optional.empty();
        String machineId = machineIdentifier.getOrCreateMachineId();
        String storedKey = settingsRepository.get(LICENSE_KEY_SETTING);
        Optional<LicensePayload> storedLicense = storedKey == null
                ? Optional.empty()
                : licenseValidator.validate(machineId, storedKey);
        if (storedLicense.isPresent()) return storedLicense;

        String profileKey = preferences.get(LICENSE_KEY_SETTING, null);
        return profileKey == null ? Optional.empty() : licenseValidator.validate(machineId, profileKey);
    }

    /** Days left in the trial. Starts the trial clock on first call if not already started. */
    public long getTrialDaysLeft() {
        if (hasClockRollback()) return 0;
        LocalDate start = earliestValidDate(
                settingsRepository.get(TRIAL_START_SETTING),
                preferences.get(TRIAL_START_SETTING, null)
        );
        if (start == null) start = LocalDate.now();

        String startValue = start.toString();
        settingsRepository.set(TRIAL_START_SETTING, startValue);
        preferences.put(TRIAL_START_SETTING, startValue);
        long elapsed = ChronoUnit.DAYS.between(start, LocalDate.now());
        return Math.max(0, TRIAL_DAYS - elapsed);
    }

    /** Days left before the active license expires and needs reactivation. Empty if not activated. */
    public Optional<Long> getDaysUntilExpiry() {
        return getActiveLicense()
                .map(payload -> ChronoUnit.DAYS.between(LocalDate.now(), payload.expiresAt()));
    }

    /**
     * Validates and persists a license in both the database and user profile.
     * Invalidates the {@link #isUsable()} cache so the next call re-evaluates.
     */
    public void activate(String activationKey) {
        String candidate = activationKey == null ? "" : activationKey.trim();
        if (!licenseValidator.isValid(machineIdentifier.getOrCreateMachineId(), candidate)) {
            throw new IllegalArgumentException("Clé d'activation invalide pour cette machine.");
        }
        settingsRepository.set(LICENSE_KEY_SETTING, candidate);
        preferences.put(LICENSE_KEY_SETTING, candidate);
        cachedIsUsable = null; // invalidate cache — next isUsable() call will re-check
    }

    /**
     * True if the app should be usable right now — licensed, or still within trial.
     *
     * <p>The result is cached for the lifetime of the JVM session to avoid
     * repeated DB queries at startup (JavaFxApplication.start → LoginController.initialize
     * both call this method). The cache is cleared by {@link #activate(String)}.
     */
    public boolean isUsable() {
        if (cachedIsUsable == null) {
            cachedIsUsable = isAlreadyActivated() || getTrialDaysLeft() > 0;
        }
        return cachedIsUsable;
    }

    private LocalDate earliestValidDate(String... values) {
        List<LocalDate> dates = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            try {
                dates.add(LocalDate.parse(value));
            } catch (RuntimeException ignored) {
                // Corrupt local data is ignored; the remaining store still protects the trial.
            }
        }
        return dates.stream().min(LocalDate::compareTo).orElse(null);
    }

    /**
     * Records the newest trusted local date in both stores. A clock set back
     * before that date makes the app unavailable until the real date catches up.
     * This does not replace online validation, but prevents the common offline
     * trial-extension and expiry-bypass technique of simply changing the clock.
     */
    private boolean hasClockRollback() {
        LocalDate today = LocalDate.now();
        LocalDate lastSeen = latestValidDate(
                settingsRepository.get(LAST_SEEN_DATE_SETTING),
                preferences.get(LAST_SEEN_DATE_SETTING, null)
        );
        if (lastSeen != null && today.isBefore(lastSeen)) return true;

        String todayValue = today.toString();
        settingsRepository.set(LAST_SEEN_DATE_SETTING, todayValue);
        preferences.put(LAST_SEEN_DATE_SETTING, todayValue);
        return false;
    }

    private LocalDate latestValidDate(String... values) {
        List<LocalDate> dates = new ArrayList<>();
        for (String value : values) {
            if (value == null || value.isBlank()) continue;
            try {
                dates.add(LocalDate.parse(value));
            } catch (RuntimeException ignored) {
                // Ignore corrupt copies; a valid copy in the other store still applies.
            }
        }
        return dates.stream().max(LocalDate::compareTo).orElse(null);
    }
}
