package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.dto.JobMatchRequest;
import com.aicareerresume.entity.JobDescription;
import com.aicareerresume.entity.JobMatch;
import com.aicareerresume.entity.Resume;
import com.aicareerresume.entity.User;
import com.aicareerresume.repository.JobDescriptionRepository;
import com.aicareerresume.repository.JobMatchRepository;
import com.aicareerresume.repository.UserRepository;
import com.aicareerresume.security.UserPrincipal;
import com.aicareerresume.service.AIService;
import com.aicareerresume.service.ResumeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/jobs")
public class JobController {

    private final AIService aiService;
    private final ResumeService resumeService;
    private final JobDescriptionRepository jdRepository;
    private final JobMatchRepository jobMatchRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public JobController(AIService aiService,
                         ResumeService resumeService,
                         JobDescriptionRepository jdRepository,
                         JobMatchRepository jobMatchRepository,
                         UserRepository userRepository) {
        this.aiService = aiService;
        this.resumeService = resumeService;
        this.jdRepository = jdRepository;
        this.jobMatchRepository = jobMatchRepository;
        this.userRepository = userRepository;
    }

    @PostMapping("/match")
    public ResponseEntity<ApiResponse<Map<String, Object>>> matchResumeWithJob(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @Valid @RequestBody JobMatchRequest req) {

        User user = userRepository.findById(currentUser.getId()).orElseThrow();
        Resume resume = resumeService.getResumeById(req.resumeId(), currentUser.getId());

        JobDescription jd = new JobDescription();
        jd.setUser(user);
        jd.setTitle(req.jobTitle());
        jd.setCompany(req.company());
        jd.setRawText(req.jobDescription());
        jdRepository.save(jd);

        Map<String, Object> matchResult = aiService.matchJobDescription(resume.getContentJson(), req.jobDescription());

        try {
            JobMatch jobMatch = new JobMatch();
            jobMatch.setUser(user);
            jobMatch.setResume(resume);
            jobMatch.setJobDescription(jd);
            jobMatch.setMatchScore((Integer) matchResult.getOrDefault("matchScore", 0));
            jobMatch.setMatchedSkillsJson(objectMapper.writeValueAsString(matchResult.get("matchedSkills")));
            jobMatch.setMissingSkillsJson(objectMapper.writeValueAsString(matchResult.get("missingSkills")));
            jobMatch.setWeakSkillsJson(objectMapper.writeValueAsString(matchResult.get("weakSkills")));
            jobMatch.setRecommendationsJson(objectMapper.writeValueAsString(matchResult.get("recommendations")));
            jobMatchRepository.save(jobMatch);
        } catch (Exception ignored) {}

        return ResponseEntity.ok(ApiResponse.ok("Job match analysis complete", matchResult));
    }

    @PostMapping("/skill-gap")
    public ResponseEntity<ApiResponse<Map<String, Object>>> analyzeSkillGap(
            @RequestBody Map<String, Object> payload) {
        List<String> missingSkills = (List<String>) payload.get("missingSkills");
        String targetRole = (String) payload.getOrDefault("targetRole", "Software Developer");
        Map<String, Object> roadmap = aiService.generateSkillGapRoadmap(missingSkills != null ? missingSkills : List.of(), targetRole);
        return ResponseEntity.ok(ApiResponse.ok("Skill gaps analyzed", roadmap));
    }
}
