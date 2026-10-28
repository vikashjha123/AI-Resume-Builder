package com.aicareerresume.dto;

import jakarta.validation.constraints.NotBlank;

public record ResumeCreateRequest(
    @NotBlank String name,
    String targetRole,
    String targetCompany,
    String template,
    String contentJson
) {}
