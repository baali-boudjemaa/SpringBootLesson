package com.example.mef.demo.license;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.Optional;

/** Ships in the app. Only ever verifies — cannot generate valid keys. */
public class LicenseValidator {

    private static final String PUBLIC_KEY_RESOURCE = "/license/public_key.der";

    private final PublicKey publicKey;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public LicenseValidator() throws Exception {
        this.publicKey = loadPublicKey();
    }

    private PublicKey loadPublicKey() throws Exception {
        try (InputStream in = getClass().getResourceAsStream(PUBLIC_KEY_RESOURCE)) {
            if (in == null) {
                throw new IllegalStateException("Missing bundled public key: " + PUBLIC_KEY_RESOURCE);
            }
            byte[] keyBytes = in.readAllBytes();
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            return KeyFactory.getInstance("RSA").generatePublic(spec);
        }
    }

    public boolean isValid(String machineId, String licenseKey) {
        return validate(machineId, licenseKey).isPresent();
    }

    /**
     * Validates the vendor signature, machine binding, plan, and expiry date.
     * Version 1 keys are {@code base64url(payload).base64url(signature)}.
     */
    public Optional<LicensePayload> validate(String machineId, String licenseKey) {
        try {
            String[] parts = licenseKey.trim().split("\\.", -1);
            if (parts.length != 2) return Optional.empty();

            byte[] payloadBytes = Base64.getUrlDecoder().decode(parts[0]);
            byte[] signatureBytes = Base64.getUrlDecoder().decode(parts[1]);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(payloadBytes);
            if (!verifier.verify(signatureBytes)) return Optional.empty();

            LicensePayload payload = objectMapper.readValue(payloadBytes, LicensePayload.class);
            boolean valid = payload.version() == 1
                    && machineId.equals(payload.machineId())
                    && payload.plan() != null
                    && payload.issuedAt() != null
                    && payload.expiresAt() != null
                    && !payload.expiresAt().isBefore(java.time.LocalDate.now());
            return valid ? Optional.of(payload) : Optional.empty();
        } catch (Exception e) {
            return Optional.empty();
        }
    }
}
