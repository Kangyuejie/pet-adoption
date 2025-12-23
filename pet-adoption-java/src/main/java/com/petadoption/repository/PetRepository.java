package com.petadoption.repository;

import com.petadoption.entity.Pet;
import com.petadoption.enums.PetGender;
import com.petadoption.enums.PetType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface PetRepository extends JpaRepository<Pet, Long> {

    List<Pet> findByOwnerIdOrderByCreatedAtDesc(Long ownerId);

    List<Pet> findByIsAvailableTrueOrderByCreatedAtDesc();

    @Query("SELECT p FROM Pet p WHERE p.isAvailable = true " +
           "AND (:petType IS NULL OR p.petType = :petType) " +
           "AND (:gender IS NULL OR p.gender = :gender) " +
           "AND (:minAge IS NULL OR p.ageMonths >= :minAge) " +
           "AND (:maxAge IS NULL OR p.ageMonths <= :maxAge) " +
           "AND (:isNeutered IS NULL OR p.isNeutered = :isNeutered) " +
           "AND (:search IS NULL OR LOWER(p.name) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.breed) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.description) LIKE LOWER(CONCAT('%', :search, '%')) " +
           "     OR LOWER(p.location) LIKE LOWER(CONCAT('%', :search, '%'))) " +
           "ORDER BY p.createdAt DESC")
    List<Pet> findByFilters(
            @Param("petType") PetType petType,
            @Param("gender") PetGender gender,
            @Param("minAge") Integer minAge,
            @Param("maxAge") Integer maxAge,
            @Param("isNeutered") Boolean isNeutered,
            @Param("search") String search
    );

    long countByPetType(PetType petType);

    long countByIsAvailableTrue();

    @Query("SELECT p.ageMonths FROM Pet p")
    List<Integer> findAllAgeMonths();
}
