package com.petadoption.service;

import com.petadoption.dto.request.PreferenceUpdateRequest;
import com.petadoption.entity.User;
import com.petadoption.entity.UserPreference;
import com.petadoption.repository.PreferenceRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class PreferenceService {

    private final PreferenceRepository preferenceRepository;

    public Map<String, Object> getPreferences(User currentUser) {
        Map<String, Object> response = new HashMap<>();

        preferenceRepository.findByUserId(currentUser.getId())
                .ifPresent(pref -> {
                    response.put("preferred_types", pref.getPreferredTypes());
                    response.put("min_age", pref.getMinAge());
                    response.put("max_age", pref.getMaxAge());
                    response.put("max_distance", pref.getMaxDistance());
                    response.put("prefer_neutered", pref.getPreferNeutered());
                });

        return response;
    }

    @Transactional
    public Map<String, String> updatePreferences(PreferenceUpdateRequest request, User currentUser) {
        UserPreference preference = preferenceRepository.findByUserId(currentUser.getId())
                .orElseGet(() -> UserPreference.builder().user(currentUser).build());

        if (request.getPreferredTypes() != null) {
            preference.setPreferredTypes(request.getPreferredTypes());
        }
        if (request.getMinAge() != null) {
            preference.setMinAge(request.getMinAge());
        }
        if (request.getMaxAge() != null) {
            preference.setMaxAge(request.getMaxAge());
        }
        if (request.getMaxDistance() != null) {
            preference.setMaxDistance(request.getMaxDistance());
        }
        if (request.getPreferNeutered() != null) {
            preference.setPreferNeutered(request.getPreferNeutered());
        }

        preferenceRepository.save(preference);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Preferences updated");
        return response;
    }
}
