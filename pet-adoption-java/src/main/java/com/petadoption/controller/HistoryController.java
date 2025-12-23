package com.petadoption.controller;

import com.petadoption.entity.User;
import com.petadoption.service.HistoryService;
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
@Tag(name = "History", description = "浏览历史接口")
public class HistoryController {

    private final HistoryService historyService;
    private final UserService userService;

    @PostMapping("/history/{petId}")
    @Operation(summary = "记录浏览")
    public ResponseEntity<Map<String, String>> recordView(
            @PathVariable Long petId,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(historyService.recordView(petId, currentUser));
    }

    @GetMapping("/history")
    @Operation(summary = "获取浏览历史")
    public ResponseEntity<List<Map<String, Object>>> getHistory(
            @RequestParam(defaultValue = "20") int limit,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(historyService.getHistory(currentUser, limit));
    }

    @DeleteMapping("/history")
    @Operation(summary = "清空浏览历史")
    public ResponseEntity<Map<String, String>> clearHistory(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(historyService.clearHistory(currentUser));
    }

    @DeleteMapping("/history/{petId}")
    @Operation(summary = "删除单条历史")
    public ResponseEntity<Map<String, String>> removeFromHistory(
            @PathVariable Long petId,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(historyService.removeFromHistory(petId, currentUser));
    }
}
