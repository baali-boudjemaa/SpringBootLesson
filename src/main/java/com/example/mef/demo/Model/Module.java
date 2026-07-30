package com.example.mef.demo.Model;

import java.util.List;

/**
 * Describes one navigable CRUD module (Students, Teachers, Classes, ...).
 *
 * @param titleKey i18n key for the nav label / page title
 * @param table    backing DB table name
 * @param orderBy  SQL ORDER BY clause used when listing rows
 * @param fields   editable fields for this module, in display order
 */
public record Module(String titleKey, String table, String orderBy, List<Field> fields) {

    public List<String> columns() {
        return fields.stream().map(Field::column).toList();
    }
}
