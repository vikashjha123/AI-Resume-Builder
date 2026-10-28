package com.aicareerresume.service;

import com.aicareerresume.dto.ResumeDto;
import com.aicareerresume.entity.Resume;

import java.io.OutputStream;
import java.util.List;

public interface ResumeService {
    List<Resume> getResumesByUserId(Long userId);
    Resume getResumeById(Long id, Long userId);
    Resume createResume(Long userId, ResumeDto req);
    Resume updateResume(Long id, Long userId, ResumeDto req);
    void deleteResume(Long id, Long userId);
    Resume cloneResume(Long id, Long userId);
    void streamPdf(Long id, Long userId, OutputStream outputStream);
    Resume tailorResumeForJob(Long id, Long userId, String jobDescription);
}