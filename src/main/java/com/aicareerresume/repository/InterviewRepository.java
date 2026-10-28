package com.aicareerresume.repository;

import com.aicareerresume.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface InterviewRepository extends JpaRepository<Interview, Long> {
    List<Interview> findByUserIdOrderByCreatedAtDesc(Long userId);
    Optional<Interview> findByIdAndUserId(Long id, Long userId);
    long countByUserIdAndStatus(Long userId, String status);

    @Query("SELECT AVG(i.overallScore) FROM Interview i WHERE i.user.id = :userId AND i.status = 'COMPLETED'")
    Double getAverageScoreByUserId(@Param("userId") Long userId);
}