package com.petadoption.service;

import com.petadoption.entity.Pet;
import com.petadoption.entity.UserPreference;
import org.springframework.stereotype.Service;

@Service
public class MatchingService {

    public int calculateMatchScore(Pet pet, UserPreference preference) {
        int score = 0;

        if (preference != null) {
            // Type matching
            if (preference.getPreferredTypes() != null && !preference.getPreferredTypes().isEmpty()) {
                String[] types = preference.getPreferredTypes().split(",");
                for (String type : types) {
                    if (pet.getPetType().getValue().equalsIgnoreCase(type.trim())) {
                        score += 30;
                        break;
                    }
                }
            }

            // Age matching
            if (preference.getMinAge() != null && preference.getMaxAge() != null) {
                if (pet.getAgeMonths() >= preference.getMinAge() &&
                        pet.getAgeMonths() <= preference.getMaxAge()) {
                    score += 25;
                }
            }

            // Neutered preference
            if (preference.getPreferNeutered() != null) {
                if (pet.getIsNeutered().equals(preference.getPreferNeutered())) {
                    score += 15;
                }
            }
        }

        // Vaccination bonus
        if (Boolean.TRUE.equals(pet.getIsVaccinated())) {
            score += 10;
        }

        // Image bonus
        if (pet.getImageUrl() != null && !pet.getImageUrl().isEmpty()) {
            score += 5;
        }

        // Description bonus
        if (pet.getDescription() != null && pet.getDescription().length() > 50) {
            score += 5;
        }

        return score;
    }
}
