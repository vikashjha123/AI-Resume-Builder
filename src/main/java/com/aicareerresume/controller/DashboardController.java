package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.dto.DashboardStatsResponse;
import com.aicareerresume.entity.JobMatch;
import com.aicareerresume.entity.MasterProfile;
import com.aicareerresume.repository.InterviewRepository;
import com.aicareerresume.repository.JobMatchRepository;
import com.aicareerresume.repository.ResumeRepository;
import com.aicareerresume.security.UserPrincipal;
import com.aicareerresume.service.ProfileService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final ProfileService profileService;
    private final ResumeRepository resumeRepository;
    private final JobMatchRepository jobMatchRepository;
    private final InterviewRepository interviewRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DashboardController(ProfileService profileService,
                               ResumeRepository resumeRepository,
                               JobMatchRepository jobMatchRepository,
                               InterviewRepository interviewRepository) {
        this.profileService = profileService;
        this.resumeRepository = resumeRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.interviewRepository = interviewRepository;
    }

    @GetMapping("/stats")
    public ResponseEntity<ApiResponse<DashboardStatsResponse>> getStats(@AuthenticationPrincipal UserPrincipal currentUser) {
        Long userId = currentUser.getId();

        // 1. Real Profile Completion Score
        MasterProfile profile = profileService.getProfileByUserId(userId);
        int completion = profileService.calculateProfileCompletion(profile);

        // 2. Real Counts from Database
        long resumeCount = resumeRepository.countByUserId(userId);
        long jobsAnalyzed = jobMatchRepository.countByUserId(userId);
        Double avgMatch = jobMatchRepository.getAverageMatchScoreByUserId(userId);
        long interviews = interviewRepository.countByUserIdAndStatus(userId, "COMPLETED");
        Double avgInterview = interviewRepository.getAverageScoreByUserId(userId);

        // 3. Real User Average ATS Score across their own resumes
        Double avgAts = resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .filter(r -> r.getAtsScore() != null && r.getAtsScore() > 0)
                .mapToInt(r -> r.getAtsScore())
                .average()
                .orElse(0.0);

        // 4. Real Missing Skills from actual Job Matches in Database (Zero hardcoded data)
        List<JobMatch> matches = jobMatchRepository.findByUserIdOrderByCreatedAtDesc(userId);
        Map<String, Integer> skillFrequency = new HashMap<>();

        for (JobMatch match : matches) {
            if (match.getMissingSkillsJson() != null && !match.getMissingSkillsJson().isBlank()) {
                try {
                    List<String> skills = objectMapper.readValue(match.getMissingSkillsJson(), new TypeReference<List<String>>() {});
                    for (String s : skills) {
                        skillFrequency.put(s, skillFrequency.getOrDefault(s, 0) + 1);
                    }
                } catch (Exception ignored) {
                    System.out.println("Result is not found");
                }
            }
        }

        // Top 5 most frequent missing skills
        List<String> realMissingSkills = skillFrequency.entrySet().stream()
                .sorted((a, b) -> b.getValue().compareTo(a.getValue()))
                .limit(5)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());

        DashboardStatsResponse stats = new DashboardStatsResponse(
                completion,
                avgAts.intValue(),
                resumeCount,
                jobsAnalyzed,
                avgMatch != null ? avgMatch.intValue() : 0,
                interviews,
                avgInterview != null ? avgInterview.intValue() : 0,
                realMissingSkills,
                Collections.emptyList()
        );

        return ResponseEntity.ok(ApiResponse.ok("Real dashboard statistics loaded", stats));
    }
}