package com.aicareerresume.service;

import java.util.List;
import java.util.Map;

public interface AIService {
    String polishBulletPoint(String rawText, String role);
    Map<String, Object> analyzeAtsCompliance(String resumeText);
    Map<String, Object> matchJobDescription(String resumeText, String jobDescription);
    Map<String, Object> generateSkillGapRoadmap(List<String> missingSkills, String targetRole);
    List<Map<String, String>> generateInterviewQuestions(String targetRole, String type, String difficulty, String resumeJson);
    Map<String, Object> evaluateAnswer(String question, String answer, String category);
    Map<String, Object> generateCareerRoadmap(String currentLevel, String targetRole, List<String> currentSkills);
    String tailorResumeContent(String masterResumeJson, String jobDescription);
    Map<String, Object> parseResumeToStructure(String resumeText);
}