package com.aicareerresume.dto;

import jakarta.validation.constraints.NotBlank;

public record StartInterviewRequest(
    Long resumeId,
    @NotBlank String targetRole,
    @NotBlank String interviewType,
    @NotBlank String difficulty
) {}
