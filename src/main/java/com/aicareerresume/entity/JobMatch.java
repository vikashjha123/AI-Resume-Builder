package com.aicareerresume.entity;

import jakarta.persistence.*;
import java.time.LocalDateTime;
import com.fasterxml.jackson.annotation.JsonIgnore;

@Entity
@Table(name = "job_matches")
public class JobMatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "resume_id")
    private Resume resume;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_description_id")
    private JobDescription jobDescription;

    private Integer matchScore;

    @Column(columnDefinition = "TEXT")
    private String matchedSkillsJson;

    @Column(columnDefinition = "TEXT")
    private String missingSkillsJson;

    @Column(columnDefinition = "TEXT")
    private String weakSkillsJson;

    @Column(columnDefinition = "LONGTEXT")
    private String recommendationsJson;

    private LocalDateTime createdAt = LocalDateTime.now();

    public JobMatch() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public Resume getResume() { return resume; }
    public void setResume(Resume resume) { this.resume = resume; }
    public JobDescription getJobDescription() { return jobDescription; }
    public void setJobDescription(JobDescription jobDescription) { this.jobDescription = jobDescription; }
    public Integer getMatchScore() { return matchScore; }
    public void setMatchScore(Integer matchScore) { this.matchScore = matchScore; }
    public String getMatchedSkillsJson() { return matchedSkillsJson; }
    public void setMatchedSkillsJson(String matchedSkillsJson) { this.matchedSkillsJson = matchedSkillsJson; }
    public String getMissingSkillsJson() { return missingSkillsJson; }
    public void setMissingSkillsJson(String missingSkillsJson) { this.missingSkillsJson = missingSkillsJson; }
    public String getWeakSkillsJson() { return weakSkillsJson; }
    public void setWeakSkillsJson(String weakSkillsJson) { this.weakSkillsJson = weakSkillsJson; }
    public String getRecommendationsJson() { return recommendationsJson; }
    public void setRecommendationsJson(String recommendationsJson) { this.recommendationsJson = recommendationsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
