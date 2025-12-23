package com.petadoption.service;

import com.petadoption.entity.Pet;
import com.petadoption.entity.PetComment;
import com.petadoption.entity.User;
import com.petadoption.enums.UserRole;
import com.petadoption.exception.BadRequestException;
import com.petadoption.exception.ResourceNotFoundException;
import com.petadoption.repository.CommentRepository;
import com.petadoption.repository.PetRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CommentService {

    private final CommentRepository commentRepository;
    private final PetRepository petRepository;

    @Transactional
    public Map<String, Object> addComment(Long petId, String content, User currentUser) {
        Pet pet = petRepository.findById(petId)
                .orElseThrow(() -> new ResourceNotFoundException("Pet not found"));

        PetComment comment = PetComment.builder()
                .pet(pet)
                .user(currentUser)
                .content(content)
                .build();

        comment = commentRepository.save(comment);

        Map<String, Object> response = new HashMap<>();
        response.put("message", "Comment added");
        response.put("id", comment.getId());
        return response;
    }

    public List<Map<String, Object>> getComments(Long petId) {
        List<PetComment> comments = commentRepository.findByPetIdOrderByCreatedAtDesc(petId);

        return comments.stream()
                .map(c -> {
                    Map<String, Object> commentMap = new HashMap<>();
                    commentMap.put("id", c.getId());
                    commentMap.put("user_id", c.getUser().getId());
                    commentMap.put("username", c.getUser().getUsername());
                    commentMap.put("content", c.getContent());
                    commentMap.put("created_at", c.getCreatedAt().toString());
                    return commentMap;
                })
                .collect(Collectors.toList());
    }

    @Transactional
    public Map<String, String> deleteComment(Long commentId, User currentUser) {
        PetComment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new ResourceNotFoundException("Comment not found"));

        if (!comment.getUser().getId().equals(currentUser.getId()) &&
                currentUser.getRole() != UserRole.ADMIN) {
            throw new BadRequestException("Not authorized");
        }

        commentRepository.delete(comment);

        Map<String, String> response = new HashMap<>();
        response.put("message", "Comment deleted");
        return response;
    }
}
