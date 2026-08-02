package com.example.mef.demo.enums;


import lombok.Getter;

@Getter
public enum BloodType {
    A_POSITIVE("A+"),
    B_POSITIVE("B+"),
    A_NEGATIVE("A-"),
    B_NEGATIVE("B-"),
    AB_POSITIVE("AB+"),
    AB_NEGATIVE("AB-"),
    O_POSITIVE("O+"),
    O_NEGATIVE("O-");

    // The display label (e.g., "A+", "O-")
    private final String label;

    // Constructor
    BloodType(String label) {
        this.label = label;
    }

    /**
     * Optional: Safe lookup method to find an enum by its string label
     */
    public static BloodType fromLabel(String label) {
        for (BloodType type : BloodType.values()) {
            if (type.label.equalsIgnoreCase(label)) {
                return type;
            }
        }
        throw new IllegalArgumentException("Unknown blood type label: " + label);
    }
}
