package com.example.backend1.analysis;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
public class TwAnalysis {
    @Id @GeneratedValue
    private Long id;

    private String app;
    private String sentiment;
    private String postType;
    private Double toxicity;

    @Column(columnDefinition = "TEXT")
    private String summary;

    private String language;
    private String version;

    @Column(columnDefinition = "TEXT")
    private String topicsJson;

    // อารมณ์
    private Double joy;
    private Double anger;
    private Double sadness;
    private Double fear;
    private Double surprise;

    // อ้างอิงต้นทาง (เช่น id โพสต์เดิม)
    private Long sourceId;

    private LocalDateTime analyzedAt;

    // -------- getters / setters --------
    public Long getId() { return id; }

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }

    public Double getToxicity() { return toxicity; }
    public void setToxicity(Double toxicity) { this.toxicity = toxicity; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getVersion() { return version; }
    public void setVersion(String version) { this.version = version; }

    public String getTopicsJson() { return topicsJson; }
    public void setTopicsJson(String topicsJson) { this.topicsJson = topicsJson; }

    public Double getJoy() { return joy; }
    public void setJoy(Double joy) { this.joy = joy; }

    public Double getAnger() { return anger; }
    public void setAnger(Double anger) { this.anger = anger; }

    public Double getSadness() { return sadness; }
    public void setSadness(Double sadness) { this.sadness = sadness; }

    public Double getFear() { return fear; }
    public void setFear(Double fear) { this.fear = fear; }

    public Double getSurprise() { return surprise; }
    public void setSurprise(Double surprise) { this.surprise = surprise; }

    public Long getSourceId() { return sourceId; }
    public void setSourceId(Long sourceId) { this.sourceId = sourceId; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
