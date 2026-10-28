package com.aicareerresume.repository;

import com.aicareerresume.entity.CareerRoadmap;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CareerRoadmapRepository extends JpaRepository<CareerRoadmap, Long> {
    Optional<CareerRoadmap> findByUserId(Long userId);
}
