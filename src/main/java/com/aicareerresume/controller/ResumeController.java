package com.aicareerresume.controller;

import com.aicareerresume.dto.ApiResponse;
import com.aicareerresume.dto.ResumeDto;
import com.aicareerresume.entity.Resume;
import com.aicareerresume.exception.BadRequestException;
import com.aicareerresume.security.UserPrincipal;
import com.aicareerresume.service.AIService;
import com.aicareerresume.service.PdfService;
import com.aicareerresume.service.ResumeService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/resumes")
public class ResumeController {

    private final ResumeService resumeService;
    private final PdfService pdfService;
    private final AIService aiService;

    public ResumeController(ResumeService resumeService, PdfService pdfService, AIService aiService) {
        this.resumeService = resumeService;
        this.pdfService = pdfService;
        this.aiService = aiService;
    }

    @PostMapping
    public ResponseEntity<ApiResponse<Resume>> createResume(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @RequestBody ResumeDto dto) {
        Resume resume = resumeService.createResume(currentUser.getId(), dto);
        return ResponseEntity.ok(ApiResponse.ok("Resume created successfully", resume));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<List<Resume>>> getResumes(@AuthenticationPrincipal UserPrincipal currentUser) {
        List<Resume> list = resumeService.getResumesByUserId(currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resumes fetched successfully", list));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Resume>> getResume(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
        Resume resume = resumeService.getResumeById(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resume fetched successfully", resume));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Resume>> updateResume(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id,
            @RequestBody ResumeDto dto) {
        Resume updated = resumeService.updateResume(id, currentUser.getId(), dto);
        return ResponseEntity.ok(ApiResponse.ok("Resume updated successfully", updated));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> deleteResume(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
        resumeService.deleteResume(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resume deleted successfully", null));
    }

    @PostMapping("/{id}/clone")
    public ResponseEntity<ApiResponse<Resume>> cloneResume(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
        Resume cloned = resumeService.cloneResume(id, currentUser.getId());
        return ResponseEntity.ok(ApiResponse.ok("Resume duplicated successfully", cloned));
    }

    @PostMapping("/{id}/generate-pdf")
    public ResponseEntity<byte[]> generatePdf(
            @AuthenticationPrincipal UserPrincipal currentUser,
            @PathVariable Long id) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try {
            resumeService.streamPdf(id, currentUser.getId(), baos);
        } catch (Exception e) {
            throw new BadRequestException("Failed to generate PDF: " + e.getMessage());
        }

        byte[] pdfBytes = baos.toByteArray();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"resume_" + id + ".pdf\"")
                .contentType(MediaType.APPLICATION_PDF)
                .body(pdfBytes);
    }

    @PostMapping("/rewrite-bullet")
    public ResponseEntity<ApiResponse<String>> rewriteBullet(@RequestBody Map<String, String> payload) {
        String rawText = payload.get("rawText");
        String role = payload.get("targetRole");
        String polished = aiService.polishBulletPoint(rawText, role);
        return ResponseEntity.ok(ApiResponse.ok("Bullet point polished successfully", polished));
    }

    @PostMapping(value = "/upload-analyze", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> uploadAndAnalyze(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("Uploaded file is empty");

        try {
            String extractedText = pdfService.extractTextFromPdf(file.getInputStream());
            Map<String, Object> analysis = aiService.analyzeAtsCompliance(extractedText);
            return ResponseEntity.ok(ApiResponse.ok("Resume audited successfully", analysis));
        } catch (Exception e) {
            throw new BadRequestException("Failed to analyze PDF resume: " + e.getMessage());
        }
    }

    @PostMapping(value = "/parse-existing", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<Map<String, Object>>> parseExistingResume(
            @RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) throw new BadRequestException("Uploaded file is empty");

        try {
            String extractedText = pdfService.extractTextFromPdf(file.getInputStream());
            Map<String, Object> parsedData = aiService.parseResumeToStructure(extractedText);
            return ResponseEntity.ok(ApiResponse.ok("Resume parsed and extracted successfully", parsedData));
        } catch (Exception e) {
            throw new BadRequestException("Could not extract data from uploaded resume: " + e.getMessage());
        }
    }
}