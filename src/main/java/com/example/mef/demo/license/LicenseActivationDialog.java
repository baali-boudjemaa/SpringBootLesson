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

    private static final String LICENSE_KEY_SETTING = "license_key";
    private static final String TRIAL_START_SETTING  = "trial_start_date";
    private static final int    TRIAL_DAYS            = 7;

    private final Preferences preferences = Preferences.userNodeForPackage(LicenseActivationDialog.class);

    public LicenseActivationDialog(MachineIdentifier machineIdentifier,
                                   LicenseValidator licenseValidator,
                                   SettingsRepository settingsRepository) {
        this.machineIdentifier = machineIdentifier;
        this.licenseValidator = licenseValidator;
        this.settingsRepository = settingsRepository;
    }

    public boolean isAlreadyActivated() {
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

    /** Validates and persists a license in both the database and user profile. */
    public void activate(String activationKey) {
        String candidate = activationKey == null ? "" : activationKey.trim();
        if (!licenseValidator.isValid(machineIdentifier.getOrCreateMachineId(), candidate)) {
            throw new IllegalArgumentException("Clé d'activation invalide pour cette machine.");
        }
        settingsRepository.set(LICENSE_KEY_SETTING, candidate);
        preferences.put(LICENSE_KEY_SETTING, candidate);
    }

    /** True if the app should be usable right now — licensed, or still within trial. */
    public boolean isUsable() {
        return isAlreadyActivated() || getTrialDaysLeft() > 0;
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
}