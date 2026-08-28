package com.example.mef.demo.Services;


import com.example.mef.demo.license.LicenseActivationDialog;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
public class TrialService {

    @Autowired private LicenseActivationDialog licenseActivationDialog;

    public synchronized LocalDate ensureStarted() {
        // The authoritative activation service owns the tamper-resistant
        // trial clock. Calling it initializes the trial if needed.
        licenseActivationDialog.getTrialDaysLeft();
        return LocalDate.now();
    }

    public long daysRemaining() {
        return licenseActivationDialog.getTrialDaysLeft();
    }

    public boolean isExpired() {
        return daysRemaining() <= 0;
    }

}
