package com.petadoption.service;

import com.petadoption.dto.request.MessageRequest;
import com.petadoption.entity.Message;
import com.petadoption.entity.Pet;
import com.petadoption.entity.User;
import com.petadoption.repository.MessageRepository;
import com.petadoption.repository.PetRepository;
import com.petadoption.repository.UserRepository;
import com.petadoption.websocket.NotificationService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MessageService {

    private final MessageRepository messageRepository;
    private final UserRepository userRepository;
    private final PetRepository petRepository;
    private final NotificationService notificationService;

    @Transactional
    public Map<String, Object> sendMessage(MessageRequest request, User currentUser) {
        User receiver = userRepository.findById(request.getReceiverId())
                .orElseThrow(() -> new RuntimeException("Receiver not found"));

        Pet pet = null;
        if (request.getPetId() != null) {
            pet = petRepository.findById(request.getPetId()).orElse(null);
        }

        Message message = Message.builder()
                .sender(currentUser)
                .receiver(receiver)
                .pet(pet)
                .content(request.getContent())
                .build();

        message = messageRepository.save(message);

        // Send real-time notification
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put("id", message.getId());
        notificationData.put("sender_id", currentUser.getId());
        notificationData.put("sender_name", currentUser.getUsername());
        notificationData.put("content", request.getContent());
        notificationData.put("pet_id", request.getPetId());
        notificationData.put("created_at", message.getCreatedAt().toString());
        notificationService.sendToUser(request.getReceiverId(), "new_message", notificationData);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Message sent");
        response.put("id", message.getId());
        return response;
    }

    public List<Map<String, Object>> getConversations(User currentUser) {
        List<Message> allMessages = messageRepository.findAllByUserId(currentUser.getId());

        // Group by conversation partner
        Map<Long, List<Message>> conversationMap = new HashMap<>();
        for (Message msg : allMessages) {
            Long partnerId = msg.getSender().getId().equals(currentUser.getId())
                    ? msg.getReceiver().getId()
                    : msg.getSender().getId();
            conversationMap.computeIfAbsent(partnerId, k -> new ArrayList<>()).add(msg);
        }

        List<Map<String, Object>> conversations = new ArrayList<>();
        for (Map.Entry<Long, List<Message>> entry : conversationMap.entrySet()) {
            Long partnerId = entry.getKey();
            List<Message> messages = entry.getValue();

            User partner = userRepository.findById(partnerId).orElse(null);
            if (partner == null) continue;

            long unreadCount = messages.stream()
                    .filter(m -> m.getReceiver().getId().equals(currentUser.getId()) && !m.getIsRead())
                    .count();

            List<Map<String, Object>> messageList = messages.stream()
                    .sorted(Comparator.comparing(Message::getCreatedAt))
                    .map(m -> {
                        Map<String, Object> msgMap = new HashMap<>();
                        msgMap.put("id", m.getId());
                        msgMap.put("sender_id", m.getSender().getId());
                        msgMap.put("receiver_id", m.getReceiver().getId());
                        msgMap.put("content", m.getContent());
                        msgMap.put("is_read", m.getIsRead());
                        msgMap.put("created_at", m.getCreatedAt().toString());
                        msgMap.put("is_mine", m.getSender().getId().equals(currentUser.getId()));
                        return msgMap;
                    })
                    .collect(Collectors.toList());

            Map<String, Object> conv = new HashMap<>();
            conv.put("partner_id", partnerId);
            conv.put("partner_name", partner.getUsername());
            conv.put("messages", messageList);
            conv.put("unread_count", unreadCount);
            conversations.add(conv);
        }

        return conversations;
    }

    @Transactional
    public Map<String, Object> getConversation(Long partnerId, User currentUser) {
        List<Message> messages = messageRepository.findConversation(currentUser.getId(), partnerId);

        // Mark as read
        for (Message msg : messages) {
            if (msg.getReceiver().getId().equals(currentUser.getId()) && !msg.getIsRead()) {
                msg.setIsRead(true);
                messageRepository.save(msg);
            }
        }

        User partner = userRepository.findById(partnerId).orElse(null);

        List<Map<String, Object>> messageList = messages.stream()
                .map(m -> {
                    Map<String, Object> msgMap = new HashMap<>();
                    msgMap.put("id", m.getId());
                    msgMap.put("sender_id", m.getSender().getId());
                    msgMap.put("content", m.getContent());
                    msgMap.put("created_at", m.getCreatedAt().toString());
                    msgMap.put("is_mine", m.getSender().getId().equals(currentUser.getId()));
                    return msgMap;
                })
                .collect(Collectors.toList());

        Map<String, Object> response = new HashMap<>();
        response.put("partner_id", partnerId);
        response.put("partner_name", partner != null ? partner.getUsername() : "Unknown");
        response.put("partner_role", partner != null ? partner.getRole().getValue() : null);
        response.put("messages", messageList);

        return response;
    }

    public Map<String, Long> getUnreadCount(User currentUser) {
        long count = messageRepository.countUnreadByReceiverId(currentUser.getId());
        Map<String, Long> response = new HashMap<>();
        response.put("unread_count", count);
        return response;
    }
}
