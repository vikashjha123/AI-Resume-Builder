package com.aicareerresume.service;

import com.aicareerresume.dto.StartInterviewRequest;
import com.aicareerresume.dto.SubmitAnswerRequest;
import com.aicareerresume.entity.Interview;
import com.aicareerresume.entity.InterviewQuestion;

public interface InterviewService {
    Interview startInterview(Long userId, StartInterviewRequest request);
    InterviewQuestion submitAnswer(Long userId, SubmitAnswerRequest request);
    Interview getInterviewResult(Long interviewId, Long userId);
}
