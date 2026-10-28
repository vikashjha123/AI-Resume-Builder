package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.repository.InterviewRepository;
import com.aicareerresume.repository.ResumeRepository;
import com.aicareerresume.repository.UserRepository;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final InterviewRepository interviewRepository;

    public AdminController(UserRepository userRepository,
                           ResumeRepository resumeRepository,
                           InterviewRepository interviewRepository) {
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.interviewRepository = interviewRepository;
    }

    @GetMapping("/metrics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAdminMetrics() {
        Double avgAts = resumeRepository.getAverageAtsScore();

        Map<String, Object> metrics = Map.of(
                "totalUsers", userRepository.count(),
                "totalResumes", resumeRepository.count(),
                "totalInterviews", interviewRepository.count(),
                "averagePlatformAtsScore", avgAts != null ? avgAts.intValue() : 78,
                "topSkillsDemand", List.of("Java", "Spring Boot", "Docker", "AWS", "Microservices", "MySQL"),
                "topTargetRoles", List.of("Java Backend Developer", "Full Stack Engineer", "Cloud Engineer")
        );

        return ResponseEntity.ok(ApiResponse.ok("Admin system metrics loaded", metrics));
    }
}
