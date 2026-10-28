package com.aicareerresume.service.impl;

import com.aicareerresume.entity.MasterProfile;
import com.aicareerresume.entity.User;
import com.aicareerresume.exception.ResourceNotFoundException;
import com.aicareerresume.repository.MasterProfileRepository;
import com.aicareerresume.repository.UserRepository;
import com.aicareerresume.service.ProfileService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProfileServiceImpl implements ProfileService {

    private final MasterProfileRepository profileRepository;
    private final UserRepository userRepository;

    public ProfileServiceImpl(MasterProfileRepository profileRepository, UserRepository userRepository) {
        this.profileRepository = profileRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public MasterProfile getProfileByUserId(Long userId) {
        return profileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = userRepository.findById(userId)
                            .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
                    MasterProfile newProf = new MasterProfile();
                    newProf.setUser(user);
                    return profileRepository.save(newProf);
                });
    }

    @Override
    @Transactional
    public MasterProfile updateProfile(Long userId, MasterProfile incoming) {
        MasterProfile existing = getProfileByUserId(userId);

        existing.setPhone(incoming.getPhone());
        existing.setLocation(incoming.getLocation());
        existing.setLinkedinUrl(incoming.getLinkedinUrl());
        existing.setGithubUrl(incoming.getGithubUrl());
        existing.setPortfolioUrl(incoming.getPortfolioUrl());
        existing.setProfessionalTitle(incoming.getProfessionalTitle());
        existing.setCareerObjective(incoming.getCareerObjective());
        existing.setAboutMe(incoming.getAboutMe());
        existing.setEducationJson(incoming.getEducationJson());
        existing.setExperienceJson(incoming.getExperienceJson());
        existing.setProjectsJson(incoming.getProjectsJson());
        existing.setSkillsJson(incoming.getSkillsJson());
        existing.setCertificationsJson(incoming.getCertificationsJson());
        existing.setAchievementsJson(incoming.getAchievementsJson());

        return profileRepository.save(existing);
    }

    @Override
    public int calculateProfileCompletion(MasterProfile p) {
        if (p == null) return 0;
        int score = 10;
        if (p.getPhone() != null && !p.getPhone().isBlank()) score += 10;
        if (p.getProfessionalTitle() != null && !p.getProfessionalTitle().isBlank()) score += 10;
        if (p.getCareerObjective() != null && !p.getCareerObjective().isBlank()) score += 10;
        if (p.getEducationJson() != null && p.getEducationJson().length() > 10) score += 20;
        if (p.getExperienceJson() != null && p.getExperienceJson().length() > 10) score += 20;
        if (p.getProjectsJson() != null && p.getProjectsJson().length() > 10) score += 10;
        if (p.getSkillsJson() != null && p.getSkillsJson().length() > 10) score += 10;
        return Math.min(score, 100);
    }
}
