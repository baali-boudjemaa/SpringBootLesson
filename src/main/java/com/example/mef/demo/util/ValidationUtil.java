package com.example.mef.demo.util;

import java.util.regex.Pattern;

public class ValidationUtil {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    private ValidationUtil() {
    }

    public static boolean isEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    public static boolean isNotBlank(String value) {
        return value != null && !value.trim().isEmpty();
    }

    public static boolean isPhone(String phone) {
        return phone != null && phone.matches("^\\+?[0-9]{8,15}$");
    }
}