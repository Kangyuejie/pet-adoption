package com.petadoption.service;

import com.petadoption.dto.response.PetResponse;
import com.petadoption.entity.Favorite;
import com.petadoption.entity.Pet;
import com.petadoption.entity.User;
import com.petadoption.repository.FavoriteRepository;
import com.petadoption.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class FavoriteService {

    private final FavoriteRepository favoriteRepository;
    private final PetRepository petRepository;

    @Transactional
    public Map<String, Object> toggleFavorite(Long petId, User currentUser) {
        Map<String, Object> response = new HashMap<>();

        favoriteRepository.findByUserIdAndPetId(currentUser.getId(), petId)
                .ifPresentOrElse(
                        favorite -> {
                            favoriteRepository.delete(favorite);
                            response.put("message", "Removed from favorites");
                            response.put("is_favorite", false);
                        },
                        () -> {
                            Pet pet = petRepository.findById(petId)
                                    .orElseThrow(() -> new RuntimeException("Pet not found"));
                            Favorite newFavorite = Favorite.builder()
                                    .user(currentUser)
                                    .pet(pet)
                                    .build();
                            favoriteRepository.save(newFavorite);
                            response.put("message", "Added to favorites");
                            response.put("is_favorite", true);
                        }
                );

        return response;
    }

    public List<PetResponse> listFavorites(User currentUser) {
        List<Favorite> favorites = favoriteRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        List<Long> petIds = favorites.stream()
                .map(f -> f.getPet().getId())
                .collect(Collectors.toList());

        return petRepository.findAllById(petIds).stream()
                .map(PetResponse::fromEntity)
                .collect(Collectors.toList());
    }
}
