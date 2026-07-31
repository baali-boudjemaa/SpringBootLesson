package com.example.mef.demo.license;

import java.util.UUID;

/** Generates a random machine ID once, persists it in the settings table, reuses it after. */
public class MachineIdentifier {

    private static final String SETTINGS_KEY = "machine_id";

    private final SettingsRepository settingsRepository; // your existing settings DAO/service

    public MachineIdentifier(SettingsRepository settingsRepository) {
        this.settingsRepository = settingsRepository;
    }

    public String getOrCreateMachineId() {
        String existing = settingsRepository.get(SETTINGS_KEY);
        if (existing != null && !existing.isBlank()) {
            return existing;
        }
        String newId = UUID.randomUUID().toString();
        settingsRepository.set(SETTINGS_KEY, newId);
        return newId;
    }
}