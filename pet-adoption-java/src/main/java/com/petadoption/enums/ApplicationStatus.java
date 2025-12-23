package com.petadoption.enums;

public enum ApplicationStatus {
    PENDING("pending"),
    APPROVED("approved"),
    REJECTED("rejected"),
    COMPLETED("completed");

    private final String value;

    ApplicationStatus(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static ApplicationStatus fromValue(String value) {
        for (ApplicationStatus status : values()) {
            if (status.value.equals(value)) {
                return status;
            }
        }
        return PENDING;
    }
}
