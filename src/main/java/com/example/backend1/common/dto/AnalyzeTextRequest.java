package com.example.backend1.common.dto;

import java.util.List;

public class AnalyzeTextRequest {
    private String text;
    private String app;
    private Long sourceId;
    private List<String> includeKeywords;
    private List<String> excludeKeywords;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public List<String> getIncludeKeywords() { return includeKeywords; }
    public void setIncludeKeywords(List<String> includeKeywords) { this.includeKeywords = includeKeywords; }

    public List<String> getExcludeKeywords() { return excludeKeywords; }
    public void setExcludeKeywords(List<String> excludeKeywords) { this.excludeKeywords = excludeKeywords; }
}
