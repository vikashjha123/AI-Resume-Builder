package com.aicareerresume.dto;

import jakarta.validation.constraints.NotBlank;

public record JobMatchRequest(
    Long resumeId,
    @NotBlank String jobTitle,
    String company,
    @NotBlank String jobDescription
) {}
