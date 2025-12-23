package com.petadoption.service;

import com.petadoption.dto.response.UserResponse;
import com.petadoption.entity.AdoptionApplication;
import com.petadoption.entity.Pet;
import com.petadoption.entity.User;
import com.petadoption.enums.ApplicationStatus;
import com.petadoption.exception.ResourceNotFoundException;
import com.petadoption.repository.ApplicationRepository;
import com.petadoption.repository.PetRepository;
import com.petadoption.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final ApplicationRepository applicationRepository;

    public User findByEmail(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public UserResponse getCurrentUser(String email) {
        User user = findByEmail(email);
        return UserResponse.fromEntity(user);
    }

    public Map<String, Object> getUserProfile(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));

        List<Pet> pets = petRepository.findByOwnerIdOrderByCreatedAtDesc(userId);
        List<Pet> availablePets = pets.stream()
                .filter(Pet::getIsAvailable)
                .limit(6)
                .toList();

        long completedAdoptions = applicationRepository.countByStatus(ApplicationStatus.COMPLETED);

        Map<String, Object> response = new HashMap<>();
        response.put("id", user.getId());
        response.put("username", user.getUsername());
        response.put("role", user.getRole().getValue());
        response.put("created_at", user.getCreatedAt().toString());
        response.put("pets_count", availablePets.size());
        response.put("completed_adoptions", completedAdoptions);
        response.put("pets", availablePets.stream()
                .map(pet -> com.petadoption.dto.response.PetResponse.fromEntity(pet))
                .toList());

        return response;
    }

    @Transactional
    public void updateProfile(String email, String phone, String address) {
        User user = findByEmail(email);
        if (phone != null) {
            user.setPhone(phone);
        }
        if (address != null) {
            user.setAddress(address);
        }
        userRepository.save(user);
    }
}
