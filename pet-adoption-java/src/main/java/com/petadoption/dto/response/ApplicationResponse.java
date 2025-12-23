package com.petadoption.dto.response;

import com.petadoption.entity.AdoptionApplication;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ApplicationResponse {
    private Long id;
    private Long petId;
    private String petName;
    private Long applicantId;
    private String applicantName;
    private String status;
    private String homeType;
    private Boolean hasYard;
    private String otherPets;
    private String experience;
    private String reason;
    private LocalDateTime createdAt;

    public static ApplicationResponse fromEntity(AdoptionApplication app) {
        return ApplicationResponse.builder()
                .id(app.getId())
                .petId(app.getPet().getId())
                .petName(app.getPet().getName())
                .applicantId(app.getApplicant().getId())
                .applicantName(app.getApplicant().getUsername())
                .status(app.getStatus().getValue())
                .homeType(app.getHomeType())
                .hasYard(app.getHasYard())
                .otherPets(app.getOtherPets())
                .experience(app.getExperience())
                .reason(app.getReason())
                .createdAt(app.getCreatedAt())
                .build();
    }
}
