package com.aicareerresume.dto;

import java.util.List;

public record DashboardStatsResponse(
    int completionPercentage,
    int atsScore,
    long resumesCount,
    long jobsAnalyzedCount,
    int averageMatchPercentage,
    long interviewsCompleted,
    int averageInterviewScore,
    List<String> commonMissingSkills,
    List<String> weakTopics
) {}
