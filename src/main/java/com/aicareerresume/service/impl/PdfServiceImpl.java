package com.aicareerresume.service.impl;

import com.aicareerresume.exception.BadRequestException;
import com.aicareerresume.service.PdfService;
import com.lowagie.text.*;
import com.lowagie.text.pdf.*;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.awt.Color;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

@Service
public class PdfServiceImpl implements PdfService {

    private static final Logger logger = LoggerFactory.getLogger(PdfServiceImpl.class);

    // ATS High-Contrast Standard Colors (No low-contrast or fancy graphics)
    private static final Color COLOR_PRIMARY = new Color(17, 24, 39);     // Deep Slate Black
    private static final Color COLOR_ACCENT = new Color(30, 64, 175);     // Dark Corporate Blue
    private static final Color COLOR_MUTED = new Color(75, 85, 99);       // Slate 600
    private static final Color COLOR_DIVIDER = new Color(229, 231, 235);  // Gray 200

    @Override
    public void generateResumePdf(Map<String, Object> data, String template, OutputStream outputStream) {
        String activeTemplate = (template != null && !template.isBlank()) ? template.toUpperCase() : "ATS_MINIMAL";

        // Compact One Page uses tighter margins (26pt), standard uses 36pt
        float margin = "COMPACT_ONE_PAGE".equals(activeTemplate) ? 26f : 36f;
        Document document = new Document(PageSize.A4, margin, margin, margin, margin);

        try {
            PdfWriter.getInstance(document, outputStream);
            document.open();

            // Font configurations based on template
            float nameSize = "COMPACT_ONE_PAGE".equals(activeTemplate) ? 17f : 20f;
            float headingSize = "COMPACT_ONE_PAGE".equals(activeTemplate) ? 10.5f : 12f;
            float bodySize = "COMPACT_ONE_PAGE".equals(activeTemplate) ? 8.8f : 9.5f;

            Font fontName = FontFactory.getFont(FontFactory.HELVETICA_BOLD, nameSize, COLOR_PRIMARY);
            Font fontTitle = FontFactory.getFont(FontFactory.HELVETICA_BOLD, bodySize + 1.5f, COLOR_ACCENT);
            Font fontHeader = FontFactory.getFont(FontFactory.HELVETICA_BOLD, headingSize, "MODERN_TECH".equals(activeTemplate) ? COLOR_ACCENT : COLOR_PRIMARY);
            Font fontSub = FontFactory.getFont(FontFactory.HELVETICA_BOLD, bodySize + 0.5f, COLOR_PRIMARY);
            Font fontBody = FontFactory.getFont(FontFactory.HELVETICA, bodySize, Color.BLACK);
            Font fontMuted = FontFactory.getFont(FontFactory.HELVETICA, bodySize - 0.5f, COLOR_MUTED);

            // Personal Contact Header
            String name = (String) data.getOrDefault("fullName", "Candidate Name");
            String title = (String) data.getOrDefault("professionalTitle", "Software Engineer");
            String email = (String) data.getOrDefault("email", "");
            String phone = (String) data.getOrDefault("phone", "");
            String location = (String) data.getOrDefault("location", "");
            String linkedin = (String) data.getOrDefault("linkedinUrl", "");
            String github = (String) data.getOrDefault("githubUrl", "");

            Paragraph namePara = new Paragraph(name, fontName);
            namePara.setAlignment("EXECUTIVE".equals(activeTemplate) ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            document.add(namePara);

            Paragraph titlePara = new Paragraph(title, fontTitle);
            titlePara.setAlignment("EXECUTIVE".equals(activeTemplate) ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            titlePara.setSpacingAfter(3);
            document.add(titlePara);

            String contactInfo = String.join("  |  ", List.of(email, phone, location, linkedin, github).stream().filter(s -> s != null && !s.isBlank()).toList());
            Paragraph contactPara = new Paragraph(contactInfo, fontMuted);
            contactPara.setAlignment("EXECUTIVE".equals(activeTemplate) ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            contactPara.setSpacingAfter(8);
            document.add(contactPara);

            addDivider(document);

            // Template Specific Section Ordering
            if ("FRESHER".equals(activeTemplate)) {
                // Freshers: Summary -> Skills -> Education -> Projects -> Experience
                renderSummary(document, data, fontHeader, fontBody);
                renderSkills(document, data, fontHeader, fontSub, fontBody);
                renderEducation(document, data, fontHeader, fontSub, fontMuted);
                renderProjects(document, data, fontHeader, fontSub, fontMuted, fontBody);
                renderExperience(document, data, fontHeader, fontSub, fontMuted, fontBody);
            } else if ("EXECUTIVE".equals(activeTemplate)) {
                // Executive: Core Competencies Summary -> Experience -> Projects -> Education -> Skills
                renderSummary(document, data, fontHeader, fontBody);
                renderExperience(document, data, fontHeader, fontSub, fontMuted, fontBody);
                renderSkills(document, data, fontHeader, fontSub, fontBody);
                renderProjects(document, data, fontHeader, fontSub, fontMuted, fontBody);
                renderEducation(document, data, fontHeader, fontSub, fontMuted);
            } else {
                // Standard ATS Minimal & Modern Tech: Summary -> Experience -> Projects -> Education -> Skills
                renderSummary(document, data, fontHeader, fontBody);
                renderExperience(document, data, fontHeader, fontSub, fontMuted, fontBody);
                renderProjects(document, data, fontHeader, fontSub, fontMuted, fontBody);
                renderEducation(document, data, fontHeader, fontSub, fontMuted);
                renderSkills(document, data, fontHeader, fontSub, fontBody);
            }

            document.close();
        } catch (Exception e) {
            logger.error("Error generating ATS PDF Resume: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to generate PDF resume: " + e.getMessage());
        }
    }

    private void renderSummary(Document doc, Map<String, Object> data, Font hFont, Font bFont) throws DocumentException {
        String summary = (String) data.getOrDefault("summary", (String) data.getOrDefault("careerObjective", (String) data.getOrDefault("aboutMe", "")));
        if (summary != null && !summary.isBlank()) {
            addSectionHeader(doc, "PROFESSIONAL SUMMARY", hFont);
            Paragraph p = new Paragraph(summary, bFont);
            p.setSpacingAfter(6);
            doc.add(p);
        }
    }

    private void renderExperience(Document doc, Map<String, Object> data, Font hFont, Font sFont, Font mFont, Font bFont) throws DocumentException {
        List<Map<String, Object>> experiences = (List<Map<String, Object>>) data.get("experience");
        if (experiences != null && !experiences.isEmpty()) {
            addSectionHeader(doc, "WORK EXPERIENCE", hFont);
            for (Map<String, Object> exp : experiences) {
                String role = (String) exp.getOrDefault("position", "Software Engineer");
                String company = (String) exp.getOrDefault("company", "Company");
                String dates = (String) exp.getOrDefault("startDate", "") + " - " + exp.getOrDefault("endDate", "Present");
                String desc = (String) exp.getOrDefault("description", "");

                PdfPTable table = new PdfPTable(2);
                table.setWidthPercentage(100);
                table.setWidths(new float[]{75, 25});

                PdfPCell c1 = new PdfPCell(new Phrase(role + " — " + company, sFont));
                c1.setBorder(Rectangle.NO_BORDER);
                PdfPCell c2 = new PdfPCell(new Phrase(dates, mFont));
                c2.setHorizontalAlignment(Element.ALIGN_RIGHT);
                c2.setBorder(Rectangle.NO_BORDER);

                table.addCell(c1);
                table.addCell(c2);
                doc.add(table);

                if (desc != null && !desc.isBlank()) {
                    Paragraph dPara = new Paragraph(desc, bFont);
                    dPara.setSpacingAfter(6);
                    doc.add(dPara);
                }
            }
        }
    }

    private void renderProjects(Document doc, Map<String, Object> data, Font hFont, Font sFont, Font mFont, Font bFont) throws DocumentException {
        List<Map<String, Object>> projects = (List<Map<String, Object>>) data.get("projects");
        if (projects != null && !projects.isEmpty()) {
            addSectionHeader(doc, "FEATURED TECHNICAL PROJECTS", hFont);
            for (Map<String, Object> proj : projects) {
                String pName = (String) proj.getOrDefault("name", "Project");
                String tech = (String) proj.getOrDefault("technologies", "");
                String desc = (String) proj.getOrDefault("description", "");

                Paragraph projHeader = new Paragraph();
                projHeader.add(new Chunk(pName, sFont));
                if (!tech.isBlank()) {
                    projHeader.add(new Chunk(" (" + tech + ")", mFont));
                }
                doc.add(projHeader);

                if (!desc.isBlank()) {
                    Paragraph pDesc = new Paragraph(desc, bFont);
                    pDesc.setSpacingAfter(6);
                    doc.add(pDesc);
                }
            }
        }
    }

    private void renderEducation(Document doc, Map<String, Object> data, Font hFont, Font sFont, Font mFont) throws DocumentException {
        List<Map<String, Object>> education = (List<Map<String, Object>>) data.get("education");
        if (education != null && !education.isEmpty()) {
            addSectionHeader(doc, "EDUCATION", hFont);
            for (Map<String, Object> edu : education) {
                String degree = (String) edu.getOrDefault("degree", "");
                String college = (String) edu.getOrDefault("college", "");
                String year = (String) edu.getOrDefault("endYear", "");
                String grade = (String) edu.getOrDefault("cgpa", "");

                Paragraph eduPara = new Paragraph();
                eduPara.add(new Chunk(degree + (!college.isBlank() ? " — " + college : ""), sFont));
                if (!year.isBlank()) eduPara.add(new Chunk(" | " + year, mFont));
                if (!grade.isBlank()) eduPara.add(new Chunk(" | CGPA/Score: " + grade, mFont));
                eduPara.setSpacingAfter(4);
                doc.add(eduPara);
            }
        }
    }

    private void renderSkills(Document doc, Map<String, Object> data, Font hFont, Font sFont, Font bFont) throws DocumentException {
        Object skillsObj = data.get("skills");
        if (skillsObj instanceof Map<?, ?> skills && !skills.isEmpty()) {
            addSectionHeader(doc, "TECHNICAL SKILLS", hFont);
            for (Map.Entry<?, ?> entry : skills.entrySet()) {
                Paragraph skPara = new Paragraph();
                skPara.add(new Chunk(entry.getKey() + ": ", sFont));
                skPara.add(new Chunk(entry.getValue().toString(), bFont));
                doc.add(skPara);
            }
        }
    }

    @Override
    public String extractTextFromPdf(InputStream inputStream) {
        try {
            byte[] bytes = inputStream.readAllBytes();
            try (PDDocument document = Loader.loadPDF(bytes)) {
                PDFTextStripper stripper = new PDFTextStripper();
                return stripper.getText(document);
            }
        } catch (Exception e) {
            logger.error("Failed to parse PDF document text", e);
            throw new BadRequestException("Uploaded PDF cannot be parsed or is password-protected: " + e.getMessage());
        }
    }

    private void addSectionHeader(Document doc, String title, Font font) throws DocumentException {
        Paragraph p = new Paragraph(title, font);
        p.setSpacingBefore(6);
        p.setSpacingAfter(2);
        doc.add(p);
        addDivider(doc);
    }

    private void addDivider(Document doc) throws DocumentException {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        PdfPCell cell = new PdfPCell();
        cell.setBorder(Rectangle.BOTTOM);
        cell.setBorderColor(COLOR_DIVIDER);
        cell.setBorderWidth(1f);
        cell.setFixedHeight(3f);
        table.addCell(cell);
        table.setSpacingAfter(4);
        doc.add(table);
    }
}