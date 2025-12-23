package com.petadoption.repository;

import com.petadoption.entity.BrowsingHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface HistoryRepository extends JpaRepository<BrowsingHistory, Long> {

    List<BrowsingHistory> findByUserIdOrderByLastViewedAtDesc(Long userId);

    Optional<BrowsingHistory> findByUserIdAndPetId(Long userId, Long petId);

    @Modifying
    @Query("DELETE FROM BrowsingHistory h WHERE h.user.id = :userId")
    void deleteAllByUserId(@Param("userId") Long userId);

    @Modifying
    @Query("DELETE FROM BrowsingHistory h WHERE h.user.id = :userId AND h.pet.id = :petId")
    void deleteByUserIdAndPetId(@Param("userId") Long userId, @Param("petId") Long petId);
}
