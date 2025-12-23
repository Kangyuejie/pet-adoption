package com.petadoption.enums;

public enum UserRole {
    ADOPTER("adopter"),
    SHELTER("shelter"),
    ADMIN("admin");

    private final String value;

    UserRole(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static UserRole fromValue(String value) {
        for (UserRole role : values()) {
            if (role.value.equals(value)) {
                return role;
            }
        }
        return ADOPTER;
    }
}
