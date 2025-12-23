package com.petadoption.repository;

import com.petadoption.entity.Message;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MessageRepository extends JpaRepository<Message, Long> {

    List<Message> findBySenderIdOrReceiverIdOrderByCreatedAtDesc(Long senderId, Long receiverId);

    @Query("SELECT m FROM Message m WHERE " +
           "(m.sender.id = :userId AND m.receiver.id = :partnerId) OR " +
           "(m.sender.id = :partnerId AND m.receiver.id = :userId) " +
           "ORDER BY m.createdAt ASC")
    List<Message> findConversation(@Param("userId") Long userId, @Param("partnerId") Long partnerId);

    @Query("SELECT COUNT(m) FROM Message m WHERE m.receiver.id = :userId AND m.isRead = false")
    long countUnreadByReceiverId(@Param("userId") Long userId);

    @Query("SELECT m FROM Message m WHERE m.sender.id = :userId OR m.receiver.id = :userId")
    List<Message> findAllByUserId(@Param("userId") Long userId);
}
