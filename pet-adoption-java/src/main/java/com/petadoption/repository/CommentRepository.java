package com.petadoption.repository;

import com.petadoption.entity.PetComment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CommentRepository extends JpaRepository<PetComment, Long> {

    List<PetComment> findByPetIdOrderByCreatedAtDesc(Long petId);
}
