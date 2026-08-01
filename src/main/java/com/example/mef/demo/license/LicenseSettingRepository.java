package com.example.mef.demo.license;

import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Renamed from "SettingRepository" (was colliding with
 * com.example.mef.demo.Repository.SettingRepository — Spring Data JPA
 * derives the bean name from the simple class name, so two interfaces
 * named "SettingRepository" in different packages both registered under
 * the bean id "settingRepository". Only one won the registration, which
 * is why JpaSettingsRepository failed to autowire with
 * "No qualifying bean of type 'com.example.mef.demo.license.SettingRepository'".
 */
public interface LicenseSettingRepository extends JpaRepository<SettingEntity, String> {
}
