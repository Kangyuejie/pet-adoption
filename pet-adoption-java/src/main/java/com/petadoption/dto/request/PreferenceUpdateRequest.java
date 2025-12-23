package com.petadoption.dto.request;

import lombok.Data;

@Data
public class PreferenceUpdateRequest {
    private String preferredTypes;
    private Integer minAge;
    private Integer maxAge;
    private Integer maxDistance;
    private Boolean preferNeutered;
}
