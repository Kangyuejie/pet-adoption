package com.petadoption.controller;

import com.petadoption.dto.request.PreferenceUpdateRequest;
import com.petadoption.entity.User;
import com.petadoption.service.PreferenceService;
import com.petadoption.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Preferences", description = "用户偏好接口")
public class PreferenceController {

    private final PreferenceService preferenceService;
    private final UserService userService;

    @GetMapping("/preferences")
    @Operation(summary = "获取用户偏好")
    public ResponseEntity<Map<String, Object>> getPreferences(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(preferenceService.getPreferences(currentUser));
    }

    @PutMapping("/preferences")
    @Operation(summary = "更新用户偏好")
    public ResponseEntity<Map<String, String>> updatePreferences(
            @RequestBody PreferenceUpdateRequest request,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(preferenceService.updatePreferences(request, currentUser));
    }
}
