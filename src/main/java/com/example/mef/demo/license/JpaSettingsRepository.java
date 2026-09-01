package com.example.mef.demo.license;

import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Wraps {@link LicenseSettingRepository} with an in-memory cache so repeated
 * {@code get()} calls for the same key (which happen many times at startup from
 * {@link LicenseActivationDialog}, {@link MachineIdentifier}, etc.) never hit
 * the database more than once per key per session.
 *
 * <p>The cache is a write-through cache: every {@link #set} call updates both
 * the DB and the in-memory map atomically, so there is never a stale read.
 */
@Component
public class JpaSettingsRepository implements SettingsRepository {

    private final LicenseSettingRepository settingRepository;

    /**
     * In-memory store. Values are wrapped in {@link Optional} so we can cache
     * "key not present in DB" as {@code Optional.empty()} and avoid a DB hit on
     * every subsequent call for that missing key.
     */
    private final ConcurrentHashMap<String, Optional<String>> cache = new ConcurrentHashMap<>();

    public JpaSettingsRepository(LicenseSettingRepository settingRepository) {
        this.settingRepository = settingRepository;
    }

    @Override
    public String get(String key) {
        return cache.computeIfAbsent(key, k ->
                settingRepository.findById(k).map(SettingEntity::getValue)
        ).orElse(null);
    }

    @Override
    public void set(String key, String value) {
        SettingEntity entity = settingRepository.findById(key)
                .map(existing -> { existing.setValue(value); return existing; })
                .orElseGet(() -> new SettingEntity(key, value));
        settingRepository.save(entity);
        // Keep the cache consistent with the DB.
        cache.put(key, Optional.ofNullable(value));
    }
}