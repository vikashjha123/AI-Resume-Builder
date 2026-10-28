package com.aicareerresume.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

public interface PdfService {
    void generateResumePdf(Map<String, Object> resumeData, String template, OutputStream outputStream);
    String extractTextFromPdf(InputStream inputStream);
}
