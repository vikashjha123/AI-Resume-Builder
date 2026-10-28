package com.aicareerresume.dto;

public record ResumeDto(
        String name,
        String targetRole,
        String targetCompany,
        String template,
        String contentJson
) {
    public String getName() {
        return name;
    }

    public String getTargetRole() {
        return targetRole;
    }

    public String getTargetCompany() {
        return targetCompany;
    }

    public String getTemplate() {
        return template;
    }

    public String getContentJson() {
        return contentJson;
    }
}