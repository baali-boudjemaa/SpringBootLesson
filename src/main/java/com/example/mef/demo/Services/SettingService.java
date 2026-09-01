package com.example.mef.demo.Services;

import com.example.mef.demo.Model.Setting;
import com.example.mef.demo.Repository.SettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Generic key/value reader-writer for the user-editable {@link Setting}
 * entity (the "Settings" module in the admin UI) — not to be confused with
 * the internal license {@code SettingEntity}/{@code SettingRepository}.
 *
 * <p>All reads are served from an in-memory write-through cache so that
 * repeated calls at startup (schoolName, billing reminder date-guard, etc.)
 * never issue more than one SQL SELECT per key per session.
 */
@Service
public class SettingService {

    private final SettingRepository settingRepository;

    /**
     * Write-through in-memory cache.
     * {@link Optional#empty()} means "key was looked up but not found in DB",
     * which lets us skip the DB on subsequent calls for missing keys too.
     */
    private final ConcurrentHashMap<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public SettingService(SettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Transactional(readOnly = true)
    public String get(String key, String defaultValue) {
        Optional<String> cached = cache.computeIfAbsent(key, k ->
                settingRepository.findById(k)
                        .map(Setting::getSettingValue)
                        .filter(v -> v != null && !v.isBlank())
        );
        return cached.orElse(defaultValue);
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
        // Keep the cache consistent with what was just written to DB.
        cache.put(key, Optional.ofNullable(value).filter(v -> !v.isBlank()));
    }
}