package com.example.mef.demo.license.tools;


import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Base64;

/** Usage: java LicenseKeyGenerator <machineId> <path-to-private_key.der> */
public class LicenseKeyGenerator {
    public static void main(String[] args) throws Exception {
        String machineId = args[0];
        byte[] keyBytes = Files.readAllBytes(Path.of(args[1]));
        PrivateKey privateKey = KeyFactory.getInstance("RSA")
                .generatePrivate(new PKCS8EncodedKeySpec(keyBytes));

        Signature signer = Signature.getInstance("SHA256withRSA");
        signer.initSign(privateKey);
        signer.update(machineId.getBytes());
        byte[] signature = signer.sign();

        System.out.println("Clé d'activation :");
        System.out.println(Base64.getUrlEncoder().withoutPadding().encodeToString(signature));
    }
}