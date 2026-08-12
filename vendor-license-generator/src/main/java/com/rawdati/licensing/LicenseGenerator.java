/*
package com.rawdati.licensing;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

*/
/**
 * Generates RSA-signed vendor license keys.
 *//*

public class LicenseGenerator {

    */
/**
     * Generates an activation key for the given machine ID.
     *
     * @param machineId            the machine identifier
     * @param privateKeyPath       path to the RSA private key (PKCS8 format)
     * @param plan                 license plan (MONTHLY or YEARLY)
     * @return LicenseKeyResult containing the activation key and expiration date
     * @throws Exception if key reading or signing fails
     *//*

    public static LicenseKeyResult generateKey(String machineId, Path privateKeyPath, LicensePlan plan)
            throws Exception {

        if (machineId == null || machineId.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine ID cannot be empty");
        }

        if (!Files.isRegularFile(privateKeyPath)) {
            throw new IllegalArgumentException("Private key file not found: " + privateKeyPath);
        }

        LocalDate issuedAt = LocalDate.now();
        LocalDate expiresAt = plan == LicensePlan.MONTHLY
                ? issuedAt.plusMonths(1)
                : issuedAt.plusYears(1);

        // Create JSON payload
        String payloadJson = String.format(
                "{\"version\":1,\"machineId\":\"%s\",\"plan\":\"%s\",\"issuedAt\":\"%s\",\"expiresAt\":\"%s\"}",
                escapeJson(machineId),
                plan,
                issuedAt,
                expiresAt
        );

        byte[] payload = payloadJson.getBytes(StandardCharsets.UTF_8);

        // Sign the payload
        PrivateKey privateKey = readPrivateKey(privateKeyPath);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(payload);
        byte[] signature = signer.sign();

        // Create activation key (Base64 URL-safe encoding without padding)
        String activationKey = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

        return new LicenseKeyResult(activationKey, issuedAt, expiresAt, plan);
    }

    */
/**
     * Reads an RSA private key from a PKCS8-encoded file.
     *//*

    private static PrivateKey readPrivateKey(Path privateKeyPath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(privateKeyPath);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    */
/**
     * Escapes JSON special characters.
     *//*

    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public enum LicensePlan {
        MONTHLY("Monthly (30 days)"),
        YEARLY("Yearly (365 days)");

        private final String displayName;

        LicensePlan(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    */
/**
     * Result of a license key generation.
     *//*

    public static class LicenseKeyResult {
        public final String activationKey;
        public final LocalDate issuedAt;
        public final LocalDate expiresAt;
        public final LicensePlan plan;

        public LicenseKeyResult(String activationKey, LocalDate issuedAt, LocalDate expiresAt, LicensePlan plan) {
            this.activationKey = activationKey;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.plan = plan;
        }
    }
}*/
package com.rawdati.licensing;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/**
 * Generates RSA-signed vendor license keys.
 */
public class LicenseGenerator {

    /**
     * Generates an activation key for the given machine ID.
     *
     * @param machineId            the machine identifier
     * @param privateKeyPath       path to the RSA private key (PKCS8 format)
     * @param plan                 license plan (MONTHLY or YEARLY)
     * @return LicenseKeyResult containing the activation key and expiration date
     * @throws Exception if key reading or signing fails
     */
    public static LicenseKeyResult generateKey(String machineId, Path privateKeyPath, LicensePlan plan)
            throws Exception {

        if (machineId == null || machineId.trim().isEmpty()) {
            throw new IllegalArgumentException("Machine ID cannot be empty");
        }

        if (!Files.isRegularFile(privateKeyPath)) {
            throw new IllegalArgumentException("Private key file not found: " + privateKeyPath);
        }

        LocalDate issuedAt = LocalDate.now();
        LocalDate expiresAt = plan == LicensePlan.MONTHLY
                ? issuedAt.plusMonths(1)
                : issuedAt.plusYears(1);

        // Create JSON payload
        String payloadJson = String.format(
                "{\"version\":1,\"machineId\":\"%s\",\"plan\":\"%s\",\"issuedAt\":\"%s\",\"expiresAt\":\"%s\"}",
                escapeJson(machineId),
                plan,
                issuedAt,
                expiresAt
        );

        byte[] payload = payloadJson.getBytes(StandardCharsets.UTF_8);

        // Sign the payload
        PrivateKey privateKey = readPrivateKey(privateKeyPath);
        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(payload);
        byte[] signature = signer.sign();

        // Create activation key (Base64 URL-safe encoding without padding)
        String activationKey = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
                + "."
                + Base64.getUrlEncoder().withoutPadding().encodeToString(signature);

        return new LicenseKeyResult(activationKey, issuedAt, expiresAt, plan);
    }

    /**
     * Reads an RSA private key from a PKCS8-encoded file.
     */
    private static PrivateKey readPrivateKey(Path privateKeyPath) throws Exception {
        byte[] keyBytes = Files.readAllBytes(privateKeyPath);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    /**
     * Escapes JSON special characters.
     */
    private static String escapeJson(String input) {
        return input.replace("\\", "\\\\")
                .replace("\"", "\\\"")
                .replace("\n", "\\n")
                .replace("\r", "\\r")
                .replace("\t", "\\t");
    }

    public enum LicensePlan {
        MONTHLY("Monthly (30 days)"),
        YEARLY("Yearly (365 days)");

        private final String displayName;

        LicensePlan(String displayName) {
            this.displayName = displayName;
        }

        public String getDisplayName() {
            return displayName;
        }
    }

    /**
     * Result of a license key generation.
     */
    public static class LicenseKeyResult {
        public final String activationKey;
        public final LocalDate issuedAt;
        public final LocalDate expiresAt;
        public final LicensePlan plan;

        public LicenseKeyResult(String activationKey, LocalDate issuedAt, LocalDate expiresAt, LicensePlan plan) {
            this.activationKey = activationKey;
            this.issuedAt = issuedAt;
            this.expiresAt = expiresAt;
            this.plan = plan;
        }
    }
}
