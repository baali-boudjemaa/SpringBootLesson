package com.example.mef.demo.license.tools;


import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/** Usage: java LicenseKeyGenerator <machineId> <path-to-private_key.der> <MONTHLY|YEARLY> */
public class LicenseKeyGenerator {
    public static void main(String[] args) throws Exception {
        if (args.length != 3) {
            System.err.println("Usage: <machineId> <path-to-private_key.der> <MONTHLY|YEARLY>");
            System.exit(2);
        }
        String machineId = args[0];
        Plan plan = Plan.valueOf(args[2].trim().toUpperCase());
        LocalDate issuedAt = LocalDate.now();
        LocalDate expiresAt = plan == Plan.MONTHLY ? issuedAt.plusMonths(1) : issuedAt.plusYears(1);
        byte[] payload = ("{\"version\":1,\"machineId\":\"" + machineId
                + "\",\"plan\":\"" + plan
                + "\",\"issuedAt\":\"" + issuedAt
                + "\",\"expiresAt\":\"" + expiresAt + "\"}").getBytes(StandardCharsets.UTF_8);
        byte[] keyBytes = Files.readAllBytes(Path.of(args[1]));
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(payload);
        byte[] signature = signer.sign();

        System.out.println("Plan : " + plan);
        System.out.println("Valide jusqu'au : " + expiresAt);
        System.out.println("Clé d'activation :");
        System.out.println(Base64.getUrlEncoder().withoutPadding().encodeToString(payload)
                + "." + Base64.getUrlEncoder().withoutPadding().encodeToString(signature));
    }

    private enum Plan { MONTHLY, YEARLY }
}
