package com.example.mef.demo.enums;

import lombok.Getter;

@Getter
public enum BloodType {

    A_POS("A+"),
    A_NEG("A-"),
    B_POS("B+"),
    B_NEG("B-"),
    AB_POS("AB+"),
    AB_NEG("AB-"),
    O_POS("O+"),
    O_NEG("O-");

    private final String label;

    BloodType(String label) {
        this.label = label;
    }

    public static BloodType fromLabel(String label) {
        for (BloodType t : values()) {
            if (t.label.equals(label)) {
                return t;
            }
        }
        throw new IllegalArgumentException(label);
    }

    @Override
    public String toString() {
        return label;
    }
}