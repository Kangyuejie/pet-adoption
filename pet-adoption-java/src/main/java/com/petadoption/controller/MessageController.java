package com.petadoption.controller;

import com.petadoption.dto.request.MessageRequest;
import com.petadoption.entity.User;
import com.petadoption.service.MessageService;
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
@Tag(name = "Messages", description = "消息接口")
public class MessageController {

    private final MessageService messageService;
    private final UserService userService;

    @PostMapping("/messages")
    @Operation(summary = "发送消息")
    public ResponseEntity<Map<String, Object>> sendMessage(
            @RequestParam Long receiver_id,
            @RequestParam String content,
            @RequestParam(required = false) Long pet_id,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());

        MessageRequest request = new MessageRequest();
        request.setReceiverId(receiver_id);
        request.setContent(content);
        request.setPetId(pet_id);

        return ResponseEntity.ok(messageService.sendMessage(request, currentUser));
    }

    @GetMapping("/messages")
    @Operation(summary = "获取会话列表")
    public ResponseEntity<List<Map<String, Object>>> getConversations(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(messageService.getConversations(currentUser));
    }

    @GetMapping("/messages/{partnerId}")
    @Operation(summary = "获取与特定用户的对话")
    public ResponseEntity<Map<String, Object>> getConversation(
            @PathVariable Long partnerId,
            Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(messageService.getConversation(partnerId, currentUser));
    }

    @GetMapping("/messages/unread/count")
    @Operation(summary = "获取未读消息数")
    public ResponseEntity<Map<String, Long>> getUnreadCount(Authentication authentication) {
        User currentUser = userService.findByEmail(authentication.getName());
        return ResponseEntity.ok(messageService.getUnreadCount(currentUser));
    }
}
