package com.example.mef.demo.util;

import javafx.scene.text.Font;

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

    /**
     * JavaFX (especially the Direct3D pipeline on Windows, and any font
     * that lacks embedded Arabic glyphs / relies on OS-level font linking)
     * can fail to render Arabic text, showing "?" for every character.
     * To make Arabic rendering independent of whatever fonts happen to be
     * installed/resolved on the target machine, we bundle a real Arabic
     * font (Noto Naskh Arabic, OFL license) as a resource and load it
     * directly at startup, instead of relying on the OS to resolve one.
     */
    private static final String ARABIC_FONT_RESOURCE = "/fonts/NotoNaskhArabic-Regular.ttf";
    private static String arabicFontFamily = "Noto Naskh Arabic"; // fallback if load fails

    static {
        try (InputStream fontStream = I18n.class.getResourceAsStream(ARABIC_FONT_RESOURCE)) {
            if (fontStream != null) {
                Font loaded = Font.loadFont(fontStream, 13);
                if (loaded != null) {
                    arabicFontFamily = loaded.getFamily();
                }
            }
        } catch (Exception ignored) {
            // Falls back to system font resolution (arabicFontFamily default) if bundling fails.
        }
    }

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

    /** Family name of the bundled Arabic font, guaranteed loaded (or the best-effort fallback name). */
    public static String getArabicFontFamily() {
        return arabicFontFamily;
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