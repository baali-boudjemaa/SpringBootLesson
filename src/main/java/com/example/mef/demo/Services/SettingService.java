package com.example.mef.demo.Services;


import com.example.mef.demo.Model.Setting;
import com.example.mef.demo.Repository.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Generic key/value reader-writer for the user-editable {@link Setting}
 * entity (the "Settings" module in the admin UI) — not to be confused with
 * the internal license {@code SettingEntity}/{@code SettingRepository}.
 */
@Service
public class SettingService {

    private final SettingRepository settingRepository;

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        return settingRepository.findById(key)
                .map(Setting::getSettingValue)
                .filter(v -> v != null && !v.isBlank())
                .orElse(defaultValue);
    }

    @Transactional(readOnly = true)
    public int getInt(String key, int defaultValue) {
        try {
            return Integer.parseInt(get(key, String.valueOf(defaultValue)).trim());
        } catch (NumberFormatException e) {
            return defaultValue;
        }
    }

    @Transactional
    public void set(String key, String value, String description) {
        Setting setting = settingRepository.findById(key)
                .orElseGet(() -> Setting.builder().settingKey(key).build());
        setting.setSettingValue(value);
        if (description != null) {
            setting.setDescription(description);
        }
        settingRepository.save(setting);
    }
}