package com.example.mef.demo.license.tools;


import java.nio.file.Files;
import java.nio.file.Path;
import java.security.KeyPair;
import java.security.KeyPairGenerator;

/** Run once, offline. Never ship this class or the private key inside the app. */
public class RsaKeyPairTool {
    public static void main(String[] args) throws Exception {
        KeyPairGenerator gen = KeyPairGenerator.getInstance("RSA");
        gen.initialize(2048);
        KeyPair pair = gen.generateKeyPair();

        Files.write(Path.of("private_key.der"), pair.getPrivate().getEncoded());
        Files.write(Path.of("public_key.der"), pair.getPublic().getEncoded());
        System.out.println("Done. Copy public_key.der into src/main/resources/license/");
        System.out.println("Keep private_key.der secret — store it somewhere safe, off this machine's Git history.");
    }
}