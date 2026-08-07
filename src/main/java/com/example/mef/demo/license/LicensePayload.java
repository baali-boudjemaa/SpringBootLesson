package com.example.mef.demo.license;

import java.time.LocalDate;

/** Vendor-signed data embedded in a version 1 activation key. */
public record LicensePayload(
        int version,
        String machineId,
        LicensePlan plan,
        LocalDate issuedAt,
        LocalDate expiresAt
) {
}
