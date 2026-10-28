package com.aicareerresume.service.impl;

import com.aicareerresume.dto.ResumeDto;
import com.aicareerresume.entity.Resume;
import com.aicareerresume.entity.User;
import com.aicareerresume.exception.ResourceNotFoundException;
import com.aicareerresume.repository.ResumeRepository;
import com.aicareerresume.repository.UserRepository;
import com.aicareerresume.service.AIService;
import com.aicareerresume.service.PdfService;
import com.aicareerresume.service.ResumeService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Service
public class ResumeServiceImpl implements ResumeService {

    private final ResumeRepository resumeRepository;
    private final UserRepository userRepository;
    private final PdfService pdfService;
    private final AIService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public ResumeServiceImpl(ResumeRepository resumeRepository,
                             UserRepository userRepository,
                             PdfService pdfService,
                             AIService aiService) {
        this.resumeRepository = resumeRepository;
        this.userRepository = userRepository;
        this.pdfService = pdfService;
        this.aiService = aiService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Resume> getResumesByUserId(Long userId) {
        return resumeRepository.findByUserIdOrderByUpdatedAtDesc(userId);
    }

    @Override
    @Transactional(readOnly = true)
    public Resume getResumeById(Long id, Long userId) {
        return resumeRepository.findByIdAndUserId(id, userId)
                .orElseThrow(() -> new ResourceNotFoundException("Resume not found with ID: " + id));
    }

    @Override
    @Transactional
    public Resume createResume(Long userId, ResumeDto req) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));

        Resume resume = new Resume();
        resume.setUser(user);
        resume.setName(req.name());
        resume.setTargetRole(req.targetRole());
        resume.setTargetCompany(req.targetCompany());
        resume.setTemplate(req.template() != null ? req.template() : "ATS_MINIMAL");
        resume.setContentJson(req.contentJson());

        if (req.contentJson() != null && !req.contentJson().isBlank()) {
            Map<String, Object> analysis = aiService.analyzeAtsCompliance(req.contentJson());
            resume.setAtsScore((Integer) analysis.getOrDefault("overallScore", 75));
        } else {
            resume.setAtsScore(75);
        }

        return resumeRepository.save(resume);
    }

    @Override
    @Transactional
    public Resume updateResume(Long id, Long userId, ResumeDto req) {
        Resume resume = getResumeById(id, userId);
        resume.setName(req.name());
        resume.setTargetRole(req.targetRole());
        resume.setTargetCompany(req.targetCompany());
        if (req.template() != null) {
            resume.setTemplate(req.template());
        }
        resume.setContentJson(req.contentJson());

        if (req.contentJson() != null && !req.contentJson().isBlank()) {
            Map<String, Object> analysis = aiService.analyzeAtsCompliance(req.contentJson());
            resume.setAtsScore((Integer) analysis.getOrDefault("overallScore", 75));
        }

        return resumeRepository.save(resume);
    }

    @Override
    @Transactional
    public void deleteResume(Long id, Long userId) {
        Resume resume = getResumeById(id, userId);
        resumeRepository.delete(resume);
    }

    @Override
    @Transactional
    public Resume cloneResume(Long id, Long userId) {
        Resume origin = getResumeById(id, userId);
        Resume clone = new Resume();
        clone.setUser(origin.getUser());
        clone.setName(origin.getName() + " (Copy)");
        clone.setTargetRole(origin.getTargetRole());
        clone.setTargetCompany(origin.getTargetCompany());
        clone.setTemplate(origin.getTemplate());
        clone.setContentJson(origin.getContentJson());
        clone.setAtsScore(origin.getAtsScore());
        clone.setJobMatchScore(origin.getJobMatchScore());
        return resumeRepository.save(clone);
    }

    @Override
    @Transactional(readOnly = true)
    public void streamPdf(Long id, Long userId, OutputStream outputStream) {
        Resume resume = getResumeById(id, userId);
        try {
            Map<String, Object> data = objectMapper.readValue(
                    resume.getContentJson() != null ? resume.getContentJson() : "{}",
                    new TypeReference<>() {}
            );
            pdfService.generateResumePdf(data, resume.getTemplate(), outputStream);
        } catch (Exception e) {
            throw new RuntimeException("Error rendering PDF stream: " + e.getMessage());
        }
    }

    @Override
    @Transactional
    public Resume tailorResumeForJob(Long id, Long userId, String jobDescription) {
        Resume resume = getResumeById(id, userId);
        String tailoredJson = aiService.tailorResumeContent(resume.getContentJson(), jobDescription);

        Resume tailored = new Resume();
        tailored.setUser(resume.getUser());
        tailored.setName(resume.getName() + " - Tailored");
        tailored.setTargetRole(resume.getTargetRole());
        tailored.setTemplate(resume.getTemplate());
        tailored.setContentJson(tailoredJson);

        Map<String, Object> matchResult = aiService.matchJobDescription(tailoredJson, jobDescription);
        tailored.setJobMatchScore((Integer) matchResult.getOrDefault("matchScore", 80));

        Map<String, Object> atsResult = aiService.analyzeAtsCompliance(tailoredJson);
        tailored.setAtsScore((Integer) atsResult.getOrDefault("overallScore", 85));

        return resumeRepository.save(tailored);
    }
}