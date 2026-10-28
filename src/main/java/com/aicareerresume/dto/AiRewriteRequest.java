package com.aicareerresume.dto;

import jakarta.validation.constraints.NotBlank;

public record AiRewriteRequest(
    @NotBlank String rawText,
    String targetRole,
    String tone
) {}
