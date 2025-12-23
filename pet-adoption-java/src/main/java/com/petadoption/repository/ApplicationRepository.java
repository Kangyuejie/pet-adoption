package com.petadoption.repository;

import com.petadoption.entity.AdoptionApplication;
import com.petadoption.enums.ApplicationStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ApplicationRepository extends JpaRepository<AdoptionApplication, Long> {

    List<AdoptionApplication> findByApplicantIdOrderByCreatedAtDesc(Long applicantId);

    List<AdoptionApplication> findByPetIdInOrderByCreatedAtDesc(List<Long> petIds);

    Optional<AdoptionApplication> findByPetIdAndApplicantIdAndStatus(
            Long petId, Long applicantId, ApplicationStatus status);

    long countByStatus(ApplicationStatus status);

    @Query("SELECT COUNT(a) FROM AdoptionApplication a WHERE a.createdAt >= :startDate")
    long countByCreatedAtAfter(@Param("startDate") LocalDateTime startDate);

    @Query("SELECT FUNCTION('MONTH', a.createdAt) as month, COUNT(a) " +
           "FROM AdoptionApplication a " +
           "WHERE a.status = :status AND FUNCTION('YEAR', a.createdAt) = :year " +
           "GROUP BY FUNCTION('MONTH', a.createdAt) " +
           "ORDER BY month")
    List<Object[]> countByStatusAndYearGroupByMonth(
            @Param("status") ApplicationStatus status,
            @Param("year") int year);
}
