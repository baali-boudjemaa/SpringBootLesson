package com.example.mef.demo.license;

/** Thin key/value contract over the app's settings table. */
public interface SettingsRepository {

    /** @return the stored value for this key, or null if not set. */
    String get(String key);

    /** Inserts or updates the value for this key. */
    void set(String key, String value);
}