package com.example.mef.demo.license;

import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.Period;

@Component
public class LicenseActivationDialog {

    private final MachineIdentifier machineIdentifier;
    private final LicenseValidator licenseValidator;
    private final SettingsRepository settingsRepository;

    private static final String LICENSE_KEY_SETTING = "license_key";
    private static final String TRIAL_START_SETTING  = "trial_start_date";
    private static final int    TRIAL_DAYS            = 7;

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
        return storedKey != null && licenseValidator.isValid(machineId, storedKey);
    }

    /** Days left in the trial. Starts the trial clock on first call if not already started. */
    public long getTrialDaysLeft() {
        String stored = settingsRepository.get(TRIAL_START_SETTING);
        LocalDate start;
        if (stored == null || stored.isBlank()) {
            start = LocalDate.now();
            settingsRepository.set(TRIAL_START_SETTING, start.toString());
        } else {
            start = LocalDate.parse(stored);
        }
        long elapsed = Period.between(start, LocalDate.now()).getDays();
        return Math.max(0, TRIAL_DAYS - elapsed);
    }

    /** True if the app should be usable right now — licensed, or still within trial. */
    public boolean isUsable() {
        return isAlreadyActivated() || getTrialDaysLeft() > 0;
    }
}