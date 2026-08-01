package com.example.mef.demo.license;

import org.springframework.stereotype.Component;

@Component
public class JpaSettingsRepository implements SettingsRepository {

    private final LicenseSettingRepository settingRepository;

    public JpaSettingsRepository(LicenseSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public String get(String key) {
        return settingRepository.findById(key)
                .map(SettingEntity::getValue)
                .orElse(null);
    }

    @Override
    public void set(String key, String value) {
        SettingEntity entity = settingRepository.findById(key)
                .map(existing -> { existing.setValue(value); return existing; })
                .orElseGet(() -> new SettingEntity(key, value));
        settingRepository.save(entity);
    }
}