package com.example.mef.demo.Services;

import com.example.mef.demo.Services.DynamicDatabaseService;
import com.example.mef.demo.license.MachineIdentifier;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.security.spec.X509EncodedKeySpec;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class LicenseService {

    private static final String PUBLIC_KEY_RESOURCE = "/license/public_key.der";

    @Autowired private DynamicDatabaseService dao;
    @Autowired private MachineIdentifier machineIdentifier;

    private PublicKey publicKey;

    private PublicKey loadPublicKey() {
        if (publicKey != null) return publicKey;
        try (InputStream in = getClass().getResourceAsStream(PUBLIC_KEY_RESOURCE)) {
            if (in == null) throw new IllegalStateException("Clé publique introuvable : " + PUBLIC_KEY_RESOURCE);
            publicKey = KeyFactory.getInstance("RSA")
                    .generatePublic(new X509EncodedKeySpec(in.readAllBytes()));
            return publicKey;
        } catch (Exception e) {
            throw new RuntimeException("Impossible de charger la clé publique.", e);
        }
    }

    public String machineId() {
        return machineIdentifier.get();
    }

    public boolean verify(String activationKey) {
        try {
            byte[] signature = Base64.getUrlDecoder().decode(activationKey.trim());
            Signature verifier = Signature.getInstance("SHA256withRSA");
            verifier.initVerify(loadPublicKey());
            verifier.update(machineId().getBytes());
            return verifier.verify(signature);
        } catch (Exception e) {
            return false;
        }
    }

    public boolean isActivated() {
        return "true".equals(getSetting("license_activated"));
    }

    /** Validates the key and, if valid, persists activation. Throws if invalid. */
    public void activate(String activationKey) {
        if (!verify(activationKey)) {
            throw new IllegalArgumentException("Clé d'activation invalide pour cette machine.");
        }
        setSetting("license_activated", "true");
        setSetting("license_key", activationKey.trim());
    }

    private String getSetting(String key) {
        List<Map<String, String>> rows = dao.findAll("settings",
                List.of("setting_key", "setting_value"), "setting_key");
        for (Map<String, String> row : rows) {
            if (key.equals(row.get("setting_key"))) return row.get("setting_value");
        }
        return null;
    }

    private void setSetting(String key, String value) {
        List<Map<String, String>> rows = dao.findAll("settings", List.of("id", "setting_key"), "setting_key");
        for (Map<String, String> row : rows) {
            if (key.equals(row.get("setting_key"))) {
                Map<String, String> values = new LinkedHashMap<>();
                values.put("id", row.get("id"));
                values.put("setting_value", value);
                dao.update("settings", List.of("setting_value"), values);
                return;
            }
        }
        Map<String, String> values = new LinkedHashMap<>();
        values.put("setting_key", key);
        values.put("setting_value", value);
        values.put("description", "");
        dao.insert("settings", List.of("setting_key", "setting_value", "description"), values);
    }
}