package com.example.mef.demo.Services;


import com.example.mef.demo.Services.DynamicDatabaseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class TrialService {

    private static final int TRIAL_DAYS = 7;
    private static final String TRIAL_START_KEY = "trial_start_date";

    @Autowired private DynamicDatabaseService dao;

    public synchronized LocalDate ensureStarted() {
        String existing = getSetting(TRIAL_START_KEY);
        if (existing != null) return LocalDate.parse(existing);

        LocalDate today = LocalDate.now();
        Map<String, String> values = new LinkedHashMap<>();
        values.put("setting_key", TRIAL_START_KEY);
        values.put("setting_value", today.toString());
        values.put("description", "Date de début de la période d'essai.");
        dao.insert("settings", List.of("setting_key", "setting_value", "description"), values);
        return today;
    }

    public long daysRemaining() {
        long elapsed = ChronoUnit.DAYS.between(ensureStarted(), LocalDate.now());
        return Math.max(0, TRIAL_DAYS - elapsed);
    }

    public boolean isExpired() {
        return daysRemaining() <= 0;
    }

    private String getSetting(String key) {
        List<Map<String, String>> rows = dao.findAll("settings",
                List.of("setting_key", "setting_value"), "setting_key");
        for (Map<String, String> row : rows) {
            if (key.equals(row.get("setting_key"))) return row.get("setting_value");
        }
        return null;
    }
}