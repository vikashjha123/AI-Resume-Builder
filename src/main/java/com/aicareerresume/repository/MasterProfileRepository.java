package com.aicareerresume.repository;

import com.aicareerresume.entity.MasterProfile;
import com.aicareerresume.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface MasterProfileRepository extends JpaRepository<MasterProfile, Long> {
    Optional<MasterProfile> findByUser(User user);
    Optional<MasterProfile> findByUserId(Long userId);
}
