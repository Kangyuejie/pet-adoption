package com.petadoption.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ApplicationCreateRequest {

    @NotNull(message = "Pet ID is required")
    private Long petId;

    private String homeType;
    private Boolean hasYard = false;
    private String otherPets;
    private String experience;
    private String reason;
}
