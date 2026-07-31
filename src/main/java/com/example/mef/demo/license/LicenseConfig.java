package com.example.mef.demo.license;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class LicenseConfig {

    @Bean
    public MachineIdentifier machineIdentifier(SettingsRepository settingsRepository) {
        return new MachineIdentifier(settingsRepository);
    }

    @Bean
    public LicenseValidator licenseValidator() throws Exception {
        return new LicenseValidator();
    }
    @Bean
    public LicenseActivationDialog licenseActivationDialog(MachineIdentifier machineIdentifier,
                                                           LicenseValidator licenseValidator,
                                                           SettingsRepository settingsRepository) {
        return new LicenseActivationDialog(machineIdentifier, licenseValidator, settingsRepository);
    }
}