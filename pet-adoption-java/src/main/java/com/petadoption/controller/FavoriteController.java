package com.petadoption.controller;

import com.petadoption.dto.response.PetResponse;
import com.petadoption.entity.User;
import com.petadoption.service.FavoriteService;
import com.petadoption.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
@Tag(name = "Favorites", description = "收藏接口")
public class FavoriteController {

    private final FavoriteService favoriteService;
    private final UserService userService;

    @PostMapping("/favorites/{petId}")
    @Operation(summary = "添加/取消收藏")
    public ResponseEntity<Map<String, Object>> toggleFavorite(
            @PathVariable Long petId,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(favoriteService.toggleFavorite(petId, currentUser));
    }

    @GetMapping("/favorites")
    @Operation(summary = "获取收藏列表")
    public ResponseEntity<List<PetResponse>> listFavorites(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(favoriteService.listFavorites(currentUser));
    }
}
