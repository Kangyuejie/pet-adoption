package com.petadoption.repository;

import com.petadoption.entity.PetImage;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PetImageRepository extends JpaRepository<PetImage, Long> {

    List<PetImage> findByPetIdOrderBySortOrder(Long petId);

    int countByPetId(Long petId);

    Optional<PetImage> findByIdAndPetId(Long id, Long petId);

    Optional<PetImage> findFirstByPetIdOrderBySortOrder(Long petId);

    @Modifying
    @Query("UPDATE PetImage pi SET pi.isPrimary = false WHERE pi.pet.id = :petId")
    void clearPrimaryByPetId(@Param("petId") Long petId);
}
