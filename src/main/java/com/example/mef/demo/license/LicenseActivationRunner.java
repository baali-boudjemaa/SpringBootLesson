package com.example.mef.demo.license;

import org.springframework.boot.CommandLineRunner;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

import java.util.Scanner;

@Component
@Order(1) // run before other CommandLineRunners that depend on activation
public class LicenseActivationRunner implements CommandLineRunner {

    private final MachineIdentifier machineIdentifier;
    private final LicenseValidator licenseValidator;
    private final SettingsRepository settingsRepository;

    private static final String LICENSE_KEY_SETTING = "license_key";

    public LicenseActivationRunner(MachineIdentifier machineIdentifier,
                                   LicenseValidator licenseValidator,
                                   SettingsRepository settingsRepository) {
        this.machineIdentifier = machineIdentifier;
        this.licenseValidator = licenseValidator;
        this.settingsRepository = settingsRepository;
    }

    @Override
    public void run(String... args) throws Exception {
        String machineId = machineIdentifier.getOrCreateMachineId();
        String storedKey = settingsRepository.get(LICENSE_KEY_SETTING);

//        if (storedKey != null && licenseValidator.isValid(machineId, storedKey)) {
//            System.out.println("License valid. Starting up.");
//            return;
//        }
//
//        System.out.println("=================================================");
//        System.out.println(" Activation required");
//        System.out.println(" Your machine ID: " + machineId);
//        System.out.println(" Send this ID to the vendor to receive a license key.");
//        System.out.println("=================================================");
//
//        try (Scanner scanner = new Scanner(System.in)) {
//            while (true) {
//                System.out.print("Enter license key: ");
//                String candidate = scanner.nextLine().trim();
//
//                if (licenseValidator.isValid(machineId, candidate)) {
//                    settingsRepository.set(LICENSE_KEY_SETTING, candidate);
//                    System.out.println("Activation successful.");
//                    return;
//                }
//                System.out.println("Invalid key for this machine. Try again.");
//            }
//        }
    }
}