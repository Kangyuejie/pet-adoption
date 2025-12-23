package com.petadoption.controller;

import com.petadoption.dto.request.PetCreateRequest;
import com.petadoption.dto.response.PetResponse;
import com.petadoption.entity.User;
import com.petadoption.service.PetService;
import com.petadoption.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Pets", description = "宠物管理接口")
public class PetController {

    private final PetService petService;
    private final UserService userService;

    @GetMapping("/pets")
    @Operation(summary = "获取宠物列表")
    public ResponseEntity<List<PetResponse>> listPets(
            @RequestParam(required = false) String pet_type,
            @RequestParam(required = false) String gender,
            @RequestParam(required = false) Integer min_age,
            @RequestParam(required = false) Integer max_age,
            @RequestParam(required = false) Boolean is_neutered,
            @RequestParam(required = false) String search,
            Authentication authentication) {
        User currentUser = null;
        if (authentication != null) {
            currentUser = userService.findByEmail(authentication.getName());
        }
        return ResponseEntity.ok(petService.listPets(pet_type, gender, min_age, max_age, is_neutered, search, currentUser));
    }

    @GetMapping("/pets/{id}")
    @Operation(summary = "获取宠物详情")
    public ResponseEntity<PetResponse> getPet(@PathVariable Long id) {
        return ResponseEntity.ok(petService.getPetById(id));
    }

    @PostMapping(value = "/pets", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "发布宠物")
    public ResponseEntity<PetResponse> createPet(
            @RequestParam String name,
            @RequestParam String pet_type,
            @RequestParam(required = false) String breed,
            @RequestParam Integer age_months,
            @RequestParam String gender,
            @RequestParam(required = false) String description,
            @RequestParam(required = false, defaultValue = "false") Boolean is_neutered,
            @RequestParam(required = false, defaultValue = "false") Boolean is_vaccinated,
            @RequestParam(required = false) String health_notes,
            @RequestParam(required = false) String location,
            @RequestParam(required = false) String adoption_requirements,
            @RequestParam(required = false) List<MultipartFile> images,
            Authentication authentication) {

        User currentUser = userService.findByEmail(authentication.getName());

        PetCreateRequest request = new PetCreateRequest();
        request.setName(name);
        request.setPetType(pet_type);
        request.setBreed(breed);
        request.setAgeMonths(age_months);
        request.setGender(gender);
        request.setDescription(description);
        request.setIsNeutered(is_neutered);
        request.setIsVaccinated(is_vaccinated);
        request.setHealthNotes(health_notes);
        request.setLocation(location);
        request.setAdoptionRequirements(adoption_requirements);

        return ResponseEntity.ok(petService.createPet(request, images, currentUser));
    }

    @PutMapping("/pets/{id}")
    @Operation(summary = "更新宠物信息")
    public ResponseEntity<PetResponse> updatePet(
            @PathVariable Long id,
            @RequestBody PetCreateRequest request,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(petService.updatePet(id, request, currentUser));
    }

    @DeleteMapping("/pets/{id}")
    @Operation(summary = "删除宠物")
    public ResponseEntity<Map<String, String>> deletePet(
            @PathVariable Long id,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        petService.deletePet(id, currentUser);
        return ResponseEntity.ok(Map.of("message", "Pet deleted"));
    }

    @GetMapping("/my-pets")
    @Operation(summary = "获取我的宠物")
    public ResponseEntity<List<PetResponse>> getMyPets(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(petService.getMyPets(currentUser));
    }

    @PostMapping(value = "/pets/{petId}/images", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "上传宠物图片")
    public ResponseEntity<Map<String, Object>> uploadImages(
            @PathVariable Long petId,
            @RequestParam List<MultipartFile> images,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(petService.uploadImages(petId, images, currentUser));
    }

    @DeleteMapping("/pets/{petId}/images/{imageId}")
    @Operation(summary = "删除宠物图片")
    public ResponseEntity<Map<String, String>> deleteImage(
            @PathVariable Long petId,
            @PathVariable Long imageId,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        petService.deleteImage(petId, imageId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Image deleted"));
    }

    @PutMapping("/pets/{petId}/images/{imageId}/primary")
    @Operation(summary = "设置主图")
    public ResponseEntity<Map<String, String>> setPrimaryImage(
            @PathVariable Long petId,
            @PathVariable Long imageId,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        petService.setPrimaryImage(petId, imageId, currentUser);
        return ResponseEntity.ok(Map.of("message", "Primary image updated"));
    }
}
