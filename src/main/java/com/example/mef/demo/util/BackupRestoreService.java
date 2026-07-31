package com.example.mef.demo.util;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Service
public class BackupRestoreService {

    @Value("${spring.datasource.url}")
    private String datasourceUrl;   // jdbc:postgresql://host:port/dbname

    @Value("${spring.datasource.username}")
    private String username;

    @Value("${spring.datasource.password}")
    private String password;

    private static final Pattern URL_PATTERN =
            Pattern.compile("jdbc:postgresql://([^:/]+):(\\d+)/([^?]+)");

    private record ConnInfo(String host, String port, String db) {}

    private ConnInfo parseUrl() {
        Matcher m = URL_PATTERN.matcher(datasourceUrl);
        if (!m.find()) {
            throw new IllegalStateException("Impossible d'analyser l'URL de connexion : " + datasourceUrl);
        }
        return new ConnInfo(m.group(1), m.group(2), m.group(3));
    }

    /**
     * Runs pg_dump into the given file (custom format, compressed).
     * Throws RuntimeException with pg_dump's stderr on failure.
     */
    public void backup(File targetFile) {
        ConnInfo c = parseUrl();
        ProcessBuilder pb = new ProcessBuilder(
                "pg_dump",
                "-h", c.host(),
                "-p", c.port(),
                "-U", username,
                "-F", "c",                 // custom format: compressed, supports selective restore
                "-f", targetFile.getAbsolutePath(),
                c.db()
        );
        pb.environment().put("PGPASSWORD", password);
        pb.redirectErrorStream(false);
        runAndCheck(pb, "pg_dump");
    }

    /**
     * Restores from the given dump file. Drops and recreates objects
     * (--clean --if-exists) so the restore is idempotent against an
     * existing database.
     */
    public void restore(File sourceFile) {
        ConnInfo c = parseUrl();
        ProcessBuilder pb = new ProcessBuilder(
                "pg_restore",
                "-h", c.host(),
                "-p", c.port(),
                "-U", username,
                "-d", c.db(),
                "--clean",
                "--if-exists",
                "--no-owner",
                sourceFile.getAbsolutePath()
        );
        pb.environment().put("PGPASSWORD", password);
        runAndCheck(pb, "pg_restore");
    }

    private void runAndCheck(ProcessBuilder pb, String toolName) {
        try {
            Process process = pb.start();
            String stderr = new String(process.getErrorStream().readAllBytes());
            int exitCode = process.waitFor();
            if (exitCode != 0) {
                throw new RuntimeException(toolName + " a échoué (code " + exitCode + ") :\n" + stderr);
            }
        } catch (IOException e) {
            throw new RuntimeException(
                    toolName + " est introuvable. Vérifiez qu'il est installé et accessible dans le PATH.", e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(toolName + " interrompu.", e);
        }
    }
}