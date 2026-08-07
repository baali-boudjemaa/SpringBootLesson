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
 * Vendor-only utility. Sign a customer's machine ID without putting the
 * private key in the customer application.
 *
 * Usage:
 *   gradlew.bat run --args="<machine-id> <private-key-path> <MONTHLY|YEARLY>"
 */
public final class VendorLicenseKeyGenerator {

    private VendorLicenseKeyGenerator() {
    }

    public static void main(String[] args) {
        if (args.length != 3) {
            System.err.println("Usage: <machine-id> <path-to-private_key.der> <MONTHLY|YEARLY>");
            System.err.println("Example: 358c... C:\\Secure\\Rawdati\\private_key.der MONTHLY");
            System.exit(2);
        }

        try {
            String machineId = args[0].trim();
            if (machineId.isEmpty()) {
                throw new IllegalArgumentException("The machine ID cannot be empty.");
            }
            LicensePlan plan = LicensePlan.valueOf(args[2].trim().toUpperCase());
            LocalDate issuedAt = LocalDate.now();
            LocalDate expiresAt = plan == LicensePlan.MONTHLY
                    ? issuedAt.plusMonths(1)
                    : issuedAt.plusYears(1);
            byte[] payload = ("{\"version\":1,\"machineId\":\"" + machineId
                    + "\",\"plan\":\"" + plan
                    + "\",\"issuedAt\":\"" + issuedAt
                    + "\",\"expiresAt\":\"" + expiresAt + "\"}")
                    .getBytes(StandardCharsets.UTF_8);

            PrivateKey privateKey = readPrivateKey(Path.of(args[1]));
            Signature signer = Signature.getInstance("SHA256withRSA");
            signer.initSign(privateKey);
            signer.update(payload);

            String activationKey = Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
                    + "."
                    + Base64.getUrlEncoder().withoutPadding().encodeToString(signer.sign());

            System.out.println();
            System.out.println("Plan: " + plan);
            System.out.println("Valid until: " + expiresAt);
            System.out.println("Activation key (send this to the customer):");
            System.out.println(activationKey);
        } catch (Exception exception) {
            System.err.println("Unable to generate activation key: " + exception.getMessage());
            System.exit(1);
        }
    }

    private static PrivateKey readPrivateKey(Path privateKeyPath) throws Exception {
        if (!Files.isRegularFile(privateKeyPath)) {
            throw new IllegalArgumentException("Private key file not found: " + privateKeyPath);
        }
        byte[] keyBytes = Files.readAllBytes(privateKeyPath);
        return KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));
    }

    private enum LicensePlan {
        MONTHLY,
        YEARLY
    }
}
