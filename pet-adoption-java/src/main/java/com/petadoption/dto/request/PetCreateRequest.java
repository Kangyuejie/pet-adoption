package com.petadoption.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class PetCreateRequest {

    @NotBlank(message = "Name is required")
    private String name;

    @NotBlank(message = "Pet type is required")
    private String petType;

    private String breed;

    @NotNull(message = "Age is required")
    @Min(value = 0, message = "Age must be non-negative")
    private Integer ageMonths;

    @NotBlank(message = "Gender is required")
    private String gender;

    private String description;
    private Boolean isNeutered = false;
    private Boolean isVaccinated = false;
    private String healthNotes;
    private String location;
    private String adoptionRequirements;
}
