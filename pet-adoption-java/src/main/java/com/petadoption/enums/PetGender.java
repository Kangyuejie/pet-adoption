package com.petadoption.enums;

public enum PetGender {
    MALE("male"),
    FEMALE("female"),
    UNKNOWN("unknown");

    private final String value;

    PetGender(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PetGender fromValue(String value) {
        for (PetGender gender : values()) {
            if (gender.value.equals(value)) {
                return gender;
            }
        }
        return UNKNOWN;
    }
}
