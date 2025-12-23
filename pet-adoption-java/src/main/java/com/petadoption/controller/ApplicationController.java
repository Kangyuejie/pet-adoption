package com.petadoption.controller;

import com.petadoption.dto.request.ApplicationCreateRequest;
import com.petadoption.dto.response.ApplicationResponse;
import com.petadoption.entity.User;
import com.petadoption.service.ApplicationService;
import com.petadoption.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Applications", description = "领养申请接口")
public class ApplicationController {

    private final ApplicationService applicationService;
    private final UserService userService;

    @PostMapping("/applications")
    @Operation(summary = "提交领养申请")
    public ResponseEntity<Map<String, Object>> createApplication(
            @Valid @RequestBody ApplicationCreateRequest request,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(applicationService.createApplication(request, currentUser));
    }

    @GetMapping("/applications")
    @Operation(summary = "获取申请列表")
    public ResponseEntity<List<ApplicationResponse>> listApplications(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(applicationService.listApplications(currentUser));
    }

    @PutMapping("/applications/{id}/status")
    @Operation(summary = "更新申请状态")
    public ResponseEntity<Map<String, String>> updateStatus(
            @PathVariable Long id,
            @RequestParam String status,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(applicationService.updateStatus(id, status, currentUser));
    }
}
