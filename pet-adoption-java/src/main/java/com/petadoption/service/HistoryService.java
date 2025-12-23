package com.petadoption.service;

import com.petadoption.dto.response.PetResponse;
import com.petadoption.entity.BrowsingHistory;
import com.petadoption.entity.Pet;
import com.petadoption.entity.User;
import com.petadoption.exception.ResourceNotFoundException;
import com.petadoption.repository.HistoryRepository;
import com.petadoption.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HistoryService {

    private final HistoryRepository historyRepository;
    private final PetRepository petRepository;

    @Transactional
    public Map<String, String> recordView(Long petId, User currentUser) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        historyRepository.findByUserIdAndPetId(currentUser.getId(), petId)
                .ifPresentOrElse(
                        history -> {
                            history.setViewCount(history.getViewCount() + 1);
                            history.setLastViewedAt(LocalDateTime.now());
                            historyRepository.save(history);
                        },
                        () -> {
                            BrowsingHistory newHistory = BrowsingHistory.builder()
                                    .user(currentUser)
                                    .pet(pet)
                                    .build();
                            historyRepository.save(newHistory);
                        }
                );

        Map<String, String> response = new HashMap<>();
        response.put("message", "View recorded");
        return response;
    }

    public List<Map<String, Object>> getHistory(User currentUser, int limit) {
        List<BrowsingHistory> histories = historyRepository
                .findByUserIdOrderByLastViewedAtDesc(currentUser.getId());

        return histories.stream()
                .limit(limit)
                .map(h -> {
                    Pet pet = h.getPet();
                    Map<String, Object> petData = new HashMap<>();

                    PetResponse petResponse = PetResponse.fromEntity(pet);
                    petData.put("id", petResponse.getId());
                    petData.put("name", petResponse.getName());
                    petData.put("pet_type", petResponse.getPetType());
                    petData.put("breed", petResponse.getBreed());
                    petData.put("age_months", petResponse.getAgeMonths());
                    petData.put("gender", petResponse.getGender());
                    petData.put("description", petResponse.getDescription());
                    petData.put("is_neutered", petResponse.getIsNeutered());
                    petData.put("is_vaccinated", petResponse.getIsVaccinated());
                    petData.put("image_url", petResponse.getImageUrl());
                    petData.put("images", petResponse.getImages());
                    petData.put("location", petResponse.getLocation());
                    petData.put("is_available", petResponse.getIsAvailable());
                    petData.put("created_at", petResponse.getCreatedAt());
                    petData.put("owner_id", petResponse.getOwnerId());
                    petData.put("view_count", h.getViewCount());
                    petData.put("last_viewed_at", h.getLastViewedAt().toString());
                    petData.put("first_viewed_at", h.getFirstViewedAt().toString());

                    return petData;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, String> clearHistory(User currentUser) {
        historyRepository.deleteAllByUserId(currentUser.getId());

        Map<String, String> response = new HashMap<>();
        response.put("message", "History cleared");
        return response;
    }

    @Transactional
    public Map<String, String> removeFromHistory(Long petId, User currentUser) {
        historyRepository.deleteByUserIdAndPetId(currentUser.getId(), petId);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Removed from history");
        return response;
    }
}
