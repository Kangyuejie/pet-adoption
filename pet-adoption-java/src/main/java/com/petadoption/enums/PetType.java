package com.petadoption.enums;

public enum PetType {
    DOG("dog"),
    CAT("cat"),
    OTHER("other");

    private final String value;

    PetType(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static PetType fromValue(String value) {
        for (PetType type : values()) {
            if (type.value.equals(value)) {
                return type;
            }
        }
        return OTHER;
    }
}
