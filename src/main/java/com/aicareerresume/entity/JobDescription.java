package com.aicareerresume.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import java.time.LocalDateTime;


@Entity
@Table(name = "job_descriptions")
public class JobDescription {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @JsonIgnore
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false)
    private String title;

    private String company;

    @Column(columnDefinition = "LONGTEXT", nullable = false)
    private String rawText;

    @Column(columnDefinition = "TEXT")
    private String extractedSkillsJson;

    private LocalDateTime createdAt = LocalDateTime.now();

    public JobDescription() {}

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getCompany() { return company; }
    public void setCompany(String company) { this.company = company; }
    public String getRawText() { return rawText; }
    public void setRawText(String rawText) { this.rawText = rawText; }
    public String getExtractedSkillsJson() { return extractedSkillsJson; }
    public void setExtractedSkillsJson(String extractedSkillsJson) { this.extractedSkillsJson = extractedSkillsJson; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}
