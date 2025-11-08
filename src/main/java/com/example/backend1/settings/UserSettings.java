package com.example.backend1.settings;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class UserSettings {
    @Id @GeneratedValue
    private Long id;

    private String theme;                 // "LIGHT" | "DARK"
    private boolean notificationsEnabled; // เปิดแจ้งเตือน?
    private int negativeThreshold;        // % negative
    @Column(columnDefinition = "TEXT")
    private String sourcesJson;           // เก็บเป็น JSON
    private String analysisScope;         // ขอบเขต
    private LocalDateTime updatedAt;

    // getters/setters
    public Long getId() { return id; }

    public String getTheme() { return theme; }
    public void setTheme(String theme) { this.theme = theme; }

    public boolean isNotificationsEnabled() { return notificationsEnabled; }
    public void setNotificationsEnabled(boolean notificationsEnabled) { this.notificationsEnabled = notificationsEnabled; }

    public int getNegativeThreshold() { return negativeThreshold; }
    public void setNegativeThreshold(int negativeThreshold) { this.negativeThreshold = negativeThreshold; }

    public String getSourcesJson() { return sourcesJson; }
    public void setSourcesJson(String sourcesJson) { this.sourcesJson = sourcesJson; }

    public String getAnalysisScope() { return analysisScope; }
    public void setAnalysisScope(String analysisScope) { this.analysisScope = analysisScope; }

    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
}
