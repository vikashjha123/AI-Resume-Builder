package com.aicareerresume.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record SubmitAnswerRequest(
    @NotNull Long questionId,
    @NotBlank String answerText
) {}
