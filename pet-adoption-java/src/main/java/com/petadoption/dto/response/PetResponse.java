package com.petadoption.dto.response;

import com.petadoption.entity.Pet;
import com.petadoption.entity.PetImage;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PetResponse {
    private Long id;
    private String name;
    private String petType;
    private String breed;
    private Integer ageMonths;
    private String gender;
    private String description;
    private Boolean isNeutered;
    private Boolean isVaccinated;
    private String healthNotes;
    private String imageUrl;
    private List<PetImageResponse> images;
    private String location;
    private String adoptionRequirements;
    private Boolean isAvailable;
    private LocalDateTime createdAt;
    private Long ownerId;
    private Integer matchScore;

    public static PetResponse fromEntity(Pet pet) {
        return fromEntity(pet, null);
    }

    public static PetResponse fromEntity(Pet pet, Integer matchScore) {
        List<PetImageResponse> imageResponses = pet.getImages() != null ?
                pet.getImages().stream()
                        .map(img -> PetImageResponse.builder()
                                .id(img.getId())
                                .imageUrl(img.getImageUrl())
                                .isPrimary(img.getIsPrimary())
                                .sortOrder(img.getSortOrder())
                                .build())
                        .collect(Collectors.toList()) :
                List.of();

        return PetResponse.builder()
                .id(pet.getId())
                .name(pet.getName())
                .petType(pet.getPetType().getValue())
                .breed(pet.getBreed())
                .ageMonths(pet.getAgeMonths())
                .gender(pet.getGender().getValue())
                .description(pet.getDescription())
                .isNeutered(pet.getIsNeutered())
                .isVaccinated(pet.getIsVaccinated())
                .healthNotes(pet.getHealthNotes())
                .imageUrl(pet.getImageUrl())
                .images(imageResponses)
                .location(pet.getLocation())
                .adoptionRequirements(pet.getAdoptionRequirements())
                .isAvailable(pet.getIsAvailable())
                .createdAt(pet.getCreatedAt())
                .ownerId(pet.getOwner().getId())
                .matchScore(matchScore)
                .build();
    }
}
