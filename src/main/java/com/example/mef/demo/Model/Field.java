package com.example.mef.demo.Model;

import com.example.mef.demo.util.I18n;

import java.util.List;

/**
 * Describes one editable field of a CRUD module.
 *
 * @param column   the DB column name
 * @param labelKey the i18n key used to resolve the display label
 * @param options  non-empty when the field must render as a ComboBox
 */
public record Field(String column, String labelKey, List<String> options) {

    public Field(String column, String labelKey) {
        this(column, labelKey, List.of());
    }

    public String label() {
        return I18n.t(labelKey, "تسجيل الحضور");
    }
}
