package com.example.mef.demo.license;

import java.util.UUID;
import java.util.prefs.Preferences;

/**
 * Generates a random installation identifier once and persists it both in the
 * settings table and in the current Windows user's profile (Java Preferences).
 * The profile copy survives an application reinstall or a recreated database.
 */
public class MachineIdentifier {

    private static final String SETTINGS_KEY = "machine_id";
    private static final String PREFERENCE_KEY = "machine_id";

    private final Preferences preferences = Preferences.userNodeForPackage(MachineIdentifier.class);

    private final SettingsRepository settingsRepository; // your existing settings DAO/service

    public MachineIdentifier(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public String getOrCreateMachineId() {
        String profileValue = preferences.get(PREFERENCE_KEY, null);
        if (profileValue != null && !profileValue.isBlank()) {
            if (!profileValue.equals(settingsRepository.get(SETTINGS_KEY))) {
                settingsRepository.set(SETTINGS_KEY, profileValue);
            }
            return profileValue;
        }

        String existing = settingsRepository.get(SETTINGS_KEY);
        if (existing != null && !existing.isBlank()) {
            preferences.put(PREFERENCE_KEY, existing);
            return existing;
        }
        String newId = UUID.randomUUID().toString();
        settingsRepository.set(SETTINGS_KEY, newId);
        preferences.put(PREFERENCE_KEY, newId);
        return newId;
    }
}
