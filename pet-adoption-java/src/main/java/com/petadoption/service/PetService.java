package com.petadoption.service;

import com.petadoption.dto.request.PetCreateRequest;
import com.petadoption.dto.response.PetResponse;
import com.petadoption.entity.Pet;
import com.petadoption.entity.PetImage;
import com.petadoption.entity.User;
import com.petadoption.entity.UserPreference;
import com.petadoption.enums.PetGender;
import com.petadoption.enums.PetType;
import com.petadoption.enums.UserRole;
import com.petadoption.exception.BadRequestException;
import com.petadoption.exception.ResourceNotFoundException;
import com.petadoption.repository.PetImageRepository;
import com.petadoption.repository.PetRepository;
import com.petadoption.repository.PreferenceRepository;
import com.petadoption.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final PetImageRepository petImageRepository;
    private final PreferenceRepository preferenceRepository;
    private final FileStorageService fileStorageService;
    private final MatchingService matchingService;
    private final NotificationService notificationService;

    public List<PetResponse> listPets(String petType, String gender, Integer minAge,
                                       Integer maxAge, Boolean isNeutered, String search,
                                       User currentUser) {
        PetType type = petType != null ? PetType.fromValue(petType) : null;
        PetGender genderEnum = gender != null ? PetGender.fromValue(gender) : null;

        List<Pet> pets = petRepository.findByFilters(type, genderEnum, minAge, maxAge, isNeutered, search);

        UserPreference preference = null;
        if (currentUser != null) {
            preference = preferenceRepository.findByUserId(currentUser.getId()).orElse(null);
        }

        final UserPreference finalPreference = preference;
        List<PetResponse> responses = pets.stream()
                .map(pet -> {
                    int score = matchingService.calculateMatchScore(pet, finalPreference);
                    return PetResponse.fromEntity(pet, score);
                })
                .sorted(Comparator.comparingInt(PetResponse::getMatchScore).reversed())
                .collect(Collectors.toList());

        return responses;
    }

    public PetResponse getPetById(Long id) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));
        return PetResponse.fromEntity(pet);
    }

    @Transactional
    public PetResponse createPet(PetCreateRequest request, List<MultipartFile> images, User owner) {
        if (owner.getRole() != UserRole.SHELTER && owner.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Only shelters can post pets");
        }

        Pet pet = Pet.builder()
                .name(request.getName())
                .petType(PetType.fromValue(request.getPetType()))
                .breed(request.getBreed())
                .ageMonths(request.getAgeMonths())
                .gender(PetGender.fromValue(request.getGender()))
                .description(request.getDescription())
                .isNeutered(request.getIsNeutered())
                .isVaccinated(request.getIsVaccinated())
                .healthNotes(request.getHealthNotes())
                .location(request.getLocation())
                .adoptionRequirements(request.getAdoptionRequirements())
                .owner(owner)
                .build();

        pet = petRepository.save(pet);

        // Handle image uploads
        if (images != null && !images.isEmpty()) {
            for (int i = 0; i < images.size(); i++) {
                MultipartFile image = images.get(i);
                if (image != null && !image.isEmpty()) {
                    try {
                        String imageUrl = fileStorageService.storeFile(image);

                        PetImage petImage = PetImage.builder()
                                .pet(pet)
                                .imageUrl(imageUrl)
                                .isPrimary(i == 0)
                                .sortOrder(i)
                                .build();
                        petImageRepository.save(petImage);

                        if (i == 0) {
                            pet.setImageUrl(imageUrl);
                        }
                    } catch (IOException e) {
                        throw new BadRequestException("Failed to upload image: " + e.getMessage());
                    }
                }
            }
            pet = petRepository.save(pet);
        }

        // Broadcast new pet notification
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("id", pet.getId());
        notificationData.put("name", pet.getName());
        notificationData.put("pet_type", pet.getPetType().getValue());
        notificationData.put("image_url", pet.getImageUrl());
        notificationService.broadcast("new_pet", notificationData);

        return PetResponse.fromEntity(pet);
    }

    @Transactional
    public PetResponse updatePet(Long id, PetCreateRequest request, User currentUser) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (!pet.getOwner().getId().equals(currentUser.getId()) &&
                currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Not authorized");
        }

        pet.setName(request.getName());
        pet.setPetType(PetType.fromValue(request.getPetType()));
        pet.setBreed(request.getBreed());
        pet.setAgeMonths(request.getAgeMonths());
        pet.setGender(PetGender.fromValue(request.getGender()));
        pet.setDescription(request.getDescription());
        pet.setIsNeutered(request.getIsNeutered());
        pet.setIsVaccinated(request.getIsVaccinated());
        pet.setHealthNotes(request.getHealthNotes());
        pet.setLocation(request.getLocation());
        pet.setAdoptionRequirements(request.getAdoptionRequirements());

        pet = petRepository.save(pet);
        return PetResponse.fromEntity(pet);
    }

    @Transactional
    public void deletePet(Long id, User currentUser) {
        Pet pet = petRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (!pet.getOwner().getId().equals(currentUser.getId()) &&
                currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Not authorized");
        }

        // Delete image files
        for (PetImage image : pet.getImages()) {
            fileStorageService.deleteFile(image.getImageUrl());
        }

        petRepository.delete(pet);
    }

    public List<PetResponse> getMyPets(User currentUser) {
        List<Pet> pets = petRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId());
        return pets.stream()
                .map(PetResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, Object> uploadImages(Long petId, List<MultipartFile> images, User currentUser) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (!pet.getOwner().getId().equals(currentUser.getId()) &&
                currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Not authorized");
        }

        int existingCount = petImageRepository.countByPetId(petId);
        List<Map<String, Object>> uploadedImages = new java.util.ArrayList<>();

        for (int i = 0; i < images.size(); i++) {
            MultipartFile image = images.get(i);
            if (image != null && !image.isEmpty()) {
                try {
                    String imageUrl = fileStorageService.storeFile(image);

                    PetImage petImage = PetImage.builder()
                            .pet(pet)
                            .imageUrl(imageUrl)
                            .isPrimary(existingCount == 0 && i == 0)
                            .sortOrder(existingCount + i)
                            .build();
                    petImage = petImageRepository.save(petImage);

                    if (existingCount == 0 && i == 0) {
                        pet.setImageUrl(imageUrl);
                        petRepository.save(pet);
                    }

                    Map<String, Object> imgData = new HashMap<>();
                    imgData.put("id", petImage.getId());
                    imgData.put("image_url", petImage.getImageUrl());
                    imgData.put("is_primary", petImage.getIsPrimary());
                    uploadedImages.add(imgData);
                } catch (IOException e) {
                    throw new BadRequestException("Failed to upload image");
                }
            }
        }

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Uploaded " + uploadedImages.size() + " images");
        response.put("images", uploadedImages);
        return response;
    }

    @Transactional
    public void deleteImage(Long petId, Long imageId, User currentUser) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (!pet.getOwner().getId().equals(currentUser.getId()) &&
                currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Not authorized");
        }

        PetImage image = petImageRepository.findByIdAndPetId(imageId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        boolean wasPrimary = image.getIsPrimary();
        fileStorageService.deleteFile(image.getImageUrl());
        petImageRepository.delete(image);

        if (wasPrimary) {
            PetImage nextImage = petImageRepository.findFirstByPetIdOrderBySortOrder(petId).orElse(null);
            if (nextImage != null) {
                nextImage.setIsPrimary(true);
                pet.setImageUrl(nextImage.getImageUrl());
                petImageRepository.save(nextImage);
            } else {
                pet.setImageUrl(null);
            }
            petRepository.save(pet);
        }
    }

    @Transactional
    public void setPrimaryImage(Long petId, Long imageId, User currentUser) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (!pet.getOwner().getId().equals(currentUser.getId()) &&
                currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Not authorized");
        }

        petImageRepository.clearPrimaryByPetId(petId);

        PetImage image = petImageRepository.findByIdAndPetId(imageId, petId)
                .orElseThrow(() -> new ResourceNotFoundException("Image not found"));

        image.setIsPrimary(true);
        pet.setImageUrl(image.getImageUrl());

        petImageRepository.save(image);
        petRepository.save(pet);
    }
}
