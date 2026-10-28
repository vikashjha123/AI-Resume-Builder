package com.aicareerresume.service.impl;

import com.aicareerresume.dto.StartInterviewRequest;
import com.aicareerresume.dto.SubmitAnswerRequest;
import com.aicareerresume.entity.*;
import com.aicareerresume.exception.ResourceNotFoundException;
import com.aicareerresume.repository.InterviewQuestionRepository;
import com.aicareerresume.repository.InterviewRepository;
import com.aicareerresume.repository.ResumeRepository;
import com.aicareerresume.repository.UserRepository;
import com.aicareerresume.service.AIService;
import com.aicareerresume.service.InterviewService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class InterviewServiceImpl implements InterviewService {

    private final InterviewRepository interviewRepository;
    private final InterviewQuestionRepository questionRepository;
    private final UserRepository userRepository;
    private final ResumeRepository resumeRepository;
    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public InterviewServiceImpl(InterviewRepository interviewRepository,
                                InterviewQuestionRepository questionRepository,
                                UserRepository userRepository,
                                ResumeRepository resumeRepository,
                                AIService aiService) {
        this.interviewRepository = interviewRepository;
        this.questionRepository = questionRepository;
        this.userRepository = userRepository;
        this.resumeRepository = resumeRepository;
        this.aiService = aiService;
    }

    @Override
    @Transactional
    public Interview startInterview(Long userId, StartInterviewRequest req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Interview interview = new Interview();
        interview.setUser(user);
        interview.setTargetRole(req.targetRole());
        interview.setInterviewType(req.interviewType());
        interview.setDifficulty(req.difficulty());
        interview.setStatus("IN_PROGRESS");

        String resumeContent = "";
        if (req.resumeId() != null) {
            resumeContent = resumeRepository.findByIdAndUserId(req.resumeId(), userId)
                    .map(Resume::getContentJson).orElse("");
        }

        List<Map<String, String>> generatedQuestions = aiService.generateInterviewQuestions(
                req.targetRole(), req.interviewType(), req.difficulty(), resumeContent
        );

        for (Map<String, String> qData : generatedQuestions) {
            InterviewQuestion q = new InterviewQuestion();
            q.setInterview(interview);
            q.setQuestionText(qData.get("questionText"));
            q.setCategory(qData.get("category"));
            q.setIdealAnswerPoints(qData.get("idealAnswerPoints"));
            interview.getQuestions().add(q);
        }

        return interviewRepository.save(interview);
    }

    @Override
    @Transactional
    public InterviewQuestion submitAnswer(Long userId, SubmitAnswerRequest req) {
        InterviewQuestion question = questionRepository.findById(req.questionId())
                .orElseThrow(() -> new ResourceNotFoundException("Question not found: " + req.questionId()));

        if (!question.getInterview().getUser().getId().equals(userId)) {
            throw new ResourceNotFoundException("Unauthorized question access");
        }

        question.setUserAnswer(req.answerText());
        Map<String, Object> eval = aiService.evaluateAnswer(question.getQuestionText(), req.answerText(), question.getCategory());

        int score = (Integer) eval.getOrDefault("score", 70);
        question.setScore(score);
        question.setFeedback((String) eval.getOrDefault("feedback", "Good response"));
        questionRepository.save(question);

        Interview interview = question.getInterview();
        boolean allAnswered = interview.getQuestions().stream().allMatch(q -> q.getUserAnswer() != null && !q.getUserAnswer().isBlank());
        if (allAnswered) {
            finalizeInterview(interview);
        }

        return question;
    }

    @Override
    @Transactional(readOnly = true)
    public Interview getInterviewResult(Long interviewId, Long userId) {
        return interviewRepository.findByIdAndUserId(interviewId, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Interview not found: " + interviewId));
    }

    private void finalizeInterview(Interview interview) {
        int total = 0;
        int techTotal = 0, techCount = 0;
        int hrTotal = 0, hrCount = 0;

        for (InterviewQuestion q : interview.getQuestions()) {
            int score = q.getScore() != null ? q.getScore() : 0;
            total += score;
            if ("Technical".equalsIgnoreCase(q.getCategory()) || "System Design".equalsIgnoreCase(q.getCategory())) {
                techTotal += score;
                techCount++;
            } else {
                hrTotal += score;
                hrCount++;
            }
        }

        int count = interview.getQuestions().isEmpty() ? 1 : interview.getQuestions().size();
        interview.setOverallScore(total / count);
        interview.setTechnicalScore(techCount > 0 ? (techTotal / techCount) : 75);
        interview.setCommunicationScore(hrCount > 0 ? (hrTotal / hrCount) : 80);
        interview.setProblemSolvingScore(Math.min(95, (total / count) + 3));
        interview.setProjectScore(Math.min(95, (total / count) - 2));
        interview.setStatus("COMPLETED");

        try {
            Map<String, Object> summary = Map.of(
                    "strengths", List.of("Effective command over technical terminology", "Solid communication cadence"),
                    "weaknesses", List.of("Provide more concrete production metrics under high scale"),
                    "recommendedTopics", List.of("System Concurrency", "Database Query Indexing")
            );
            interview.setFeedbackSummaryJson(objectMapper.writeValueAsString(summary));
        } catch (Exception ignored) {}

        interviewRepository.save(interview);
    }
}
