package com.aicareerresume.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "interviews")
public class Interview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String targetRole;
    private String interviewType;
    private String difficulty;

    private Integer overallScore = 0;
    private Integer technicalScore = 0;
    private Integer communicationScore = 0;
    private Integer problemSolvingScore = 0;
    private Integer projectScore = 0;

    @Column(columnDefinition = "LONGTEXT")
    private String feedbackSummaryJson;

    private String status = "IN_PROGRESS";

    @OneToMany(mappedBy = "interview", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<InterviewQuestion> questions = new ArrayList<>();

    private LocalDateTime createdAt = LocalDateTime.now();

    public Interview() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTargetRole() { return targetRole; }
    public void setTargetRole(String targetRole) { this.targetRole = targetRole; }
    public String getInterviewType() { return interviewType; }
    public void setInterviewType(String interviewType) { this.interviewType = interviewType; }
    public String getDifficulty() { return difficulty; }
    public void setDifficulty(String difficulty) { this.difficulty = difficulty; }
    public Integer getOverallScore() { return overallScore; }
    public void setOverallScore(Integer overallScore) { this.overallScore = overallScore; }
    public Integer getTechnicalScore() { return technicalScore; }
    public void setTechnicalScore(Integer technicalScore) { this.technicalScore = technicalScore; }
    public Integer getCommunicationScore() { return communicationScore; }
    public void setCommunicationScore(Integer communicationScore) { this.communicationScore = communicationScore; }
    public Integer getProblemSolvingScore() { return problemSolvingScore; }
    public void setProblemSolvingScore(Integer problemSolvingScore) { this.problemSolvingScore = problemSolvingScore; }
    public Integer getProjectScore() { return projectScore; }
    public void setProjectScore(Integer projectScore) { this.projectScore = projectScore; }
    public String getFeedbackSummaryJson() { return feedbackSummaryJson; }
    public void setFeedbackSummaryJson(String feedbackSummaryJson) { this.feedbackSummaryJson = feedbackSummaryJson; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public List<InterviewQuestion> getQuestions() { return questions; }
    public void setQuestions(List<InterviewQuestion> questions) { this.questions = questions; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
