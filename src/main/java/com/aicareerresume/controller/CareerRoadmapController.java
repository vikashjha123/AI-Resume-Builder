package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.entity.MasterProfile;
import com.aicareerresume.security.UserPrincipal;
import com.aicareerresume.service.AIService;
import com.aicareerresume.service.ProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/career-roadmap")
public class CareerRoadmapController {

    private final AIService aiService;
    private final ProfileService profileService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public CareerRoadmapController(AIService aiService, ProfileService profileService) {
        this.aiService = aiService;
        this.profileService = profileService;
    }

    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> getRoadmap(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestParam(required = false) String targetRole) {

        Long userId = currentUser.getId();
        MasterProfile profile = profileService.getProfileByUserId(userId);

        String role = (targetRole != null && !targetRole.isBlank())
                ? targetRole.trim()
                : (profile != null && profile.getProfessionalTitle() != null && !profile.getProfessionalTitle().isBlank()
                ? profile.getProfessionalTitle()
                : "Java Backend Developer");

        List<String> skillsList = new ArrayList<>();
        if (profile != null && profile.getSkillsJson() != null && !profile.getSkillsJson().isBlank()) {
            try {
                Map<String, String> map = objectMapper.readValue(profile.getSkillsJson(), new TypeReference<>() {});
                skillsList.addAll(map.values().stream().filter(v -> v != null && !v.isBlank()).toList());
            } catch (Exception ignored) {}
        }

        Map<String, Object> roadmap = aiService.generateCareerRoadmap("Junior Professional", role, skillsList);
        return ResponseEntity.ok(ApiResponse.ok("Real career roadmap retrieved successfully", roadmap));
    }
}