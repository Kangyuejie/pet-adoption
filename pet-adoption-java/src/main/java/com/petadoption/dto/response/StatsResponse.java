package com.petadoption.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StatsResponse {
    private Long totalPets;
    private Long availablePets;
    private Long totalApplications;
    private Long completedAdoptions;
    private Map<String, Long> petsByType;
}
