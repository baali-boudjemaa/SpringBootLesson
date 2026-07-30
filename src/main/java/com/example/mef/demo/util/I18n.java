package com.example.mef.demo.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.PropertyResourceBundle;
import java.util.ResourceBundle;

/**
 * Lightweight internationalisation helper.
 * Supports FR (default) and AR (Arabic, RTL).
 *
 * Usage:
 *   I18n.setLocale(new Locale("ar"));
 *   String label = I18n.t("nav.students");  // → "الطلاب"
 */
public final class I18n {

    private static Locale  currentLocale = Locale.FRENCH;
    private static ResourceBundle bundle  = load(currentLocale);

    private I18n() {}

    /* ── Public API ───────────────────────────────────────────── */

    public static void setLocale(Locale locale) {
        currentLocale = locale;
        bundle        = load(locale);
    }

    public static Locale getLocale() { return currentLocale; }

    public static boolean isRTL() {
        return "ar".equals(currentLocale.getLanguage());
    }

    /**
     * Returns the translated string for the given key.
     * Falls back to the key itself if not found.
     */
    public static String t(String key) {
        try {
            return bundle.getString(key);
        } catch (Exception e) {
            return key;
        }
    }

    /* ── Loader — UTF-8 safe ──────────────────────────────────── */

    private static ResourceBundle load(Locale locale) {
        String lang     = locale.getLanguage(); // "fr" or "ar"
        String filename = "i18n/messages_" + lang + ".properties";
        try {
            InputStream is = I18n.class.getClassLoader().getResourceAsStream(filename);
            if (is == null) {
                // fallback to French
                is = I18n.class.getClassLoader().getResourceAsStream("i18n/messages_fr.properties");
            }
            return new PropertyResourceBundle(new InputStreamReader(is, StandardCharsets.UTF_8));
        } catch (IOException e) {
            throw new RuntimeException("Could not load i18n bundle: " + filename, e);
        }
    }
}
