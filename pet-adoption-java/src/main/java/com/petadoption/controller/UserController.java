package com.petadoption.controller;

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
@Tag(name = "Users", description = "用户接口")
public class UserController {

    private final UserService userService;

    @GetMapping("/users/{userId}")
    @Operation(summary = "获取用户资料")
    public ResponseEntity<Map<String, Object>> getUserProfile(@PathVariable Long userId) {
        return ResponseEntity.ok(userService.getUserProfile(userId));
    }

    @PutMapping("/profile")
    @Operation(summary = "更新个人资料")
    public ResponseEntity<Map<String, String>> updateProfile(
            @RequestParam(required = false) String phone,
            @RequestParam(required = false) String address,
            Authentication authentication) {
        userService.updateProfile(authentication.getName(), phone, address);
        return ResponseEntity.ok(Map.of("message", "Profile updated"));
    }
}
