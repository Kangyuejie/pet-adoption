package com.petadoption.controller;

import com.petadoption.entity.User;
import com.petadoption.service.CommentService;
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
@Tag(name = "Comments", description = "评论接口")
public class CommentController {

    private final CommentService commentService;
    private final UserService userService;

    @PostMapping("/pets/{petId}/comments")
    @Operation(summary = "添加评论")
    public ResponseEntity<Map<String, Object>> addComment(
            @PathVariable Long petId,
            @RequestParam String content,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(commentService.addComment(petId, content, currentUser));
    }

    @GetMapping("/pets/{petId}/comments")
    @Operation(summary = "获取评论列表")
    public ResponseEntity<List<Map<String, Object>>> getComments(@PathVariable Long petId) {
        return ResponseEntity.ok(commentService.getComments(petId));
    }

    @DeleteMapping("/comments/{commentId}")
    @Operation(summary = "删除评论")
    public ResponseEntity<Map<String, String>> deleteComment(
            @PathVariable Long commentId,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(commentService.deleteComment(commentId, currentUser));
    }
}
