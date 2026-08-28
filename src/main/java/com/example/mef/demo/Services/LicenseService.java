package com.example.mef.demo.Services;

import com.example.mef.demo.license.LicenseActivationDialog;
import com.example.mef.demo.license.LicenseValidator;
import com.example.mef.demo.license.MachineIdentifier;
import org.springframework.stereotype.Service;

@Service
public class LicenseService {

    private final MachineIdentifier machineIdentifier;
    private final LicenseValidator licenseValidator;
    private final LicenseActivationDialog licenseActivationDialog;

    public LicenseService(MachineIdentifier machineIdentifier,
                          LicenseValidator licenseValidator,
                          LicenseActivationDialog licenseActivationDialog) {
        this.machineIdentifier = machineIdentifier;
        this.licenseValidator = licenseValidator;
        this.licenseActivationDialog = licenseActivationDialog;
    }

    public String machineId() {
        return machineIdentifier.getOrCreateMachineId();
    }

    public boolean verify(String activationKey) {
        return licenseValidator.isValid(machineId(), activationKey);
    }

    public boolean isActivated() {
        return licenseActivationDialog.isAlreadyActivated();
    }

    /** Validates the key and, if valid, persists activation. Throws if invalid. */
    public void activate(String activationKey) {
        licenseActivationDialog.activate(activationKey);
    }
}
