package com.example.mef.demo.Model;

import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.SuperBuilder;

/**
 * User-editable application setting (Settings module in the admin UI).
 * Deliberately a separate table from com.example.mef.demo.license.SettingEntity,
 * which stores internal license/activation state and must not be exposed
 * or editable through this generic-looking key/value screen.
 */
@Getter
@Setter
@Entity
@Table(name = "Setting")
@NoArgsConstructor
@AllArgsConstructor
@SuperBuilder
public class Setting {

    @Id
    @Column(name = "setting_key", nullable = false, updatable = false)
    private String settingKey;

    @Column(name = "setting_value", length = 2000)
    private String settingValue;

    private String description;
}