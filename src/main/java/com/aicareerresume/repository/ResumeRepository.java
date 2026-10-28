package com.aicareerresume.repository;

import com.aicareerresume.entity.Resume;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ResumeRepository extends JpaRepository<Resume, Long> {
    List<Resume> findByUserIdOrderByUpdatedAtDesc(Long userId);
    Optional<Resume> findByIdAndUserId(Long id, Long userId);
    long countByUserId(Long userId);

    @Query("SELECT AVG(r.atsScore) FROM Resume r WHERE r.atsScore > 0")
    Double getAverageAtsScore();
}
