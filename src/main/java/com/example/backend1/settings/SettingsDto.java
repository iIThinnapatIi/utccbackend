package com.example.backend1.settings;

import java.time.LocalDateTime;
import java.util.List;

public class SettingsDto {
    private String theme;
    private Boolean notificationsEnabled;
    private Integer negativeThreshold;
    private List<String> sources;
    private String analysisScope;

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public Boolean getNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(Boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public Integer getNegativeThreshold() { return negativeThreshold; }
    public void setNegativeThreshold(Integer negativeThreshold) { this.negativeThreshold = negativeThreshold; }

    public List<String> getSources() { return sources; }
    public void setSources(List<String> sources) { this.sources = sources; }

    public String getAnalysisScope() { return analysisScope; }
    public void setAnalysisScope(String analysisScope) { this.analysisScope = analysisScope; }

    public void setUpdatedAt(LocalDateTime updatedAt) {
    }
}
