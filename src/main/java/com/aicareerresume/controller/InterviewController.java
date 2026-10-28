package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.dto.StartInterviewRequest;
import com.aicareerresume.dto.SubmitAnswerRequest;
import com.aicareerresume.entity.Interview;
import com.aicareerresume.entity.InterviewQuestion;
import com.aicareerresume.security.UserPrincipal;
import com.aicareerresume.service.InterviewService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/interviews")
public class InterviewController {

    private final InterviewService interviewService;

    public InterviewController(InterviewService interviewService) {
        this.interviewService = interviewService;
    }

    @PostMapping("/start")
    public ResponseEntity<ApiResponse<Interview>> startInterview(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                 @Valid @RequestBody StartInterviewRequest req) {
        Interview interview = interviewService.startInterview(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Interview started", interview));
    }

    @PostMapping("/answer")
    public ResponseEntity<ApiResponse<InterviewQuestion>> submitAnswer(@AuthenticationPrincipal UserPrincipal currentUser,
                                                                       @Valid @RequestBody SubmitAnswerRequest req) {
        InterviewQuestion question = interviewService.submitAnswer(currentUser.getId(), req);
        return ResponseEntity.ok(ApiResponse.ok("Answer recorded", question));
    }

    @GetMapping("/{id}/result")
    public ResponseEntity<ApiResponse<Interview>> getResult(@AuthenticationPrincipal UserPrincipal currentUser,
                                                            @PathVariable Long id) {
        Interview interview = interviewService.getInterviewResult(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Interview score ready", interview));
    }
}
