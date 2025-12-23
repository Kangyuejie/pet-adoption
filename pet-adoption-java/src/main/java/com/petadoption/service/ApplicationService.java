package com.petadoption.service;

import com.petadoption.dto.request.ApplicationCreateRequest;
import com.petadoption.dto.response.ApplicationResponse;
import com.petadoption.entity.AdoptionApplication;
import com.petadoption.entity.Pet;
import com.petadoption.entity.User;
import com.petadoption.enums.ApplicationStatus;
import com.petadoption.enums.UserRole;
import com.petadoption.exception.BadRequestException;
import com.petadoption.exception.ResourceNotFoundException;
import com.petadoption.repository.ApplicationRepository;
import com.petadoption.repository.PetRepository;
import com.petadoption.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ApplicationService {

    private final ApplicationRepository applicationRepository;
    private final PetRepository petRepository;
    private final NotificationService notificationService;

    @Transactional
    public Map<String, Object> createApplication(ApplicationCreateRequest request, User currentUser) {
        Pet pet = petRepository.findById(request.getPetId())
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        if (!pet.getIsAvailable()) {
            throw new BadRequestException("Pet is not available for adoption");
        }

        // Check for existing pending application
        applicationRepository.findByPetIdAndApplicantIdAndStatus(
                request.getPetId(), currentUser.getId(), ApplicationStatus.PENDING
        ).ifPresent(app -> {
            throw new BadRequestException("You already have a pending application for this pet");
        });

        AdoptionApplication application = AdoptionApplication.builder()
                .pet(pet)
                .applicant(currentUser)
                .homeType(request.getHomeType())
                .hasYard(request.getHasYard())
                .otherPets(request.getOtherPets())
                .experience(request.getExperience())
                .reason(request.getReason())
                .build();

        application = applicationRepository.save(application);

        // Notify pet owner
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("application_id", application.getId());
        notificationData.put("pet_id", pet.getId());
        notificationData.put("pet_name", pet.getName());
        notificationData.put("applicant_name", currentUser.getUsername());
        notificationService.sendToUser(pet.getOwner().getId(), "new_application", notificationData);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Application submitted");
        response.put("id", application.getId());
        return response;
    }

    public List<ApplicationResponse> listApplications(User currentUser) {
        List<AdoptionApplication> applications;

        if (currentUser.getRole() == UserRole.ADOPTER) {
            applications = applicationRepository.findByApplicantIdOrderByCreatedAtDesc(currentUser.getId());
        } else {
            List<Pet> pets = petRepository.findByOwnerIdOrderByCreatedAtDesc(currentUser.getId());
            List<Long> petIds = pets.stream().map(Pet::getId).collect(Collectors.toList());
            applications = applicationRepository.findByPetIdInOrderByCreatedAtDesc(petIds);
        }

        return applications.stream()
                .map(ApplicationResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, String> updateStatus(Long applicationId, String status, User currentUser) {
        AdoptionApplication application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("Application not found"));

        Pet pet = application.getPet();
        if (!pet.getOwner().getId().equals(currentUser.getId()) &&
                currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Not authorized");
        }

        ApplicationStatus newStatus = ApplicationStatus.fromValue(status);
        application.setStatus(newStatus);

        if (newStatus == ApplicationStatus.COMPLETED) {
            pet.setIsAvailable(false);
            petRepository.save(pet);
        }

        applicationRepository.save(application);

        // Notify applicant
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("application_id", application.getId());
        notificationData.put("pet_id", pet.getId());
        notificationData.put("pet_name", pet.getName());
        notificationData.put("status", status);
        notificationService.sendToUser(application.getApplicant().getId(), "application_status_update", notificationData);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Status updated");
        return response;
    }
}
