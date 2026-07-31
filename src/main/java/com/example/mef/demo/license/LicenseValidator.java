package com.example.mef.demo.license;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;

/** Ships in the app. Only ever verifies — cannot generate valid keys. */
public class LicenseValidator {

    private static final String PUBLIC_KEY_RESOURCE = "/license/public_key.der";

    private final PublicKey publicKey;

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

    /**
     * @param machineId  the ID from MachineIdentifier
     * @param licenseKey the Base64URL signature the customer was given
     * @return true only if licenseKey is a valid RSA signature of machineId
     */
    public boolean isValid(String machineId, String licenseKey) {
        try {
            byte[] signatureBytes = Base64.getUrlDecoder().decode(licenseKey);
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(publicKey);
            verifier.update(machineId.getBytes(StandardCharsets.UTF_8));
            return verifier.verify(signatureBytes);
        } catch (Exception e) {
            // Any parsing/format error means the key is invalid — never throw out to caller
            return false;
        }
    }
}