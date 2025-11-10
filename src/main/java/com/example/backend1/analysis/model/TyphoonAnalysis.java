package com.example.backend1.analysis.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "typhoon_analysis")
@Access(AccessType.FIELD) // ให้ Hibernate แมปจาก "field" อย่างเดียว
public class TyphoonAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 32, nullable = false)
    private String app;

    @Column(name = "source_table", length = 64, nullable = false)
    private String sourceTable;

    @Column(name = "source_id", length = 128, nullable = false)
    private String sourceId;

    @Column(name = "post_type", length = 32)
    private String postType;

    @Column(length = 8)
    private String language;

    @Column(length = 32)
    private String sentiment;

    @Column(name = "sentiment_score", precision = 8, scale = 4)
    private BigDecimal sentimentScore;

    @Column(length = 128)
    private String topic;

    @Column(name = "topics_json", columnDefinition = "TEXT")
    private String topicsJson;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(name = "emotion_joy", precision = 8, scale = 4)
    private BigDecimal emotionJoy;

    @Column(name = "emotion_fear", precision = 8, scale = 4)
    private BigDecimal emotionFear;

    @Column(name = "emotion_sadness", precision = 8, scale = 4)
    private BigDecimal emotionSadness;

    @Column(name = "emotion_surprise", precision = 8, scale = 4)
    private BigDecimal emotionSurprise;

    @Column(precision = 8, scale = 4)
    private BigDecimal toxicity;

    @Column(length = 16)
    private String nsfw;

    @Column(name = "faculty_code", length = 32)
    private String facultyCode;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // ฟิลด์จริงของคอลัมน์ analyzed_at
    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    // -------- getters/setters อื่น ๆ --------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }

    public String getSourceTable() { return sourceTable; }
    public void setSourceTable(String sourceTable) { this.sourceTable = sourceTable; }

    public String getSourceId() { return sourceId; }
    public void setSourceId(String sourceId) { this.sourceId = sourceId; }

    public String getPostType() { return postType; }
    public void setPostType(String postType) { this.postType = postType; }

    public String getLanguage() { return language; }
    public void setLanguage(String language) { this.language = language; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public BigDecimal getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(BigDecimal sentimentScore) { this.sentimentScore = sentimentScore; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getTopicsJson() { return topicsJson; }
    public void setTopicsJson(String topicsJson) { this.topicsJson = topicsJson; }

    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }

    public BigDecimal getEmotionJoy() { return emotionJoy; }
    public void setEmotionJoy(BigDecimal emotionJoy) { this.emotionJoy = emotionJoy; }

    public BigDecimal getEmotionFear() { return emotionFear; }
    public void setEmotionFear(BigDecimal emotionFear) { this.emotionFear = emotionFear; }

    public BigDecimal getEmotionSadness() { return emotionSadness; }
    public void setEmotionSadness(BigDecimal emotionSadness) { this.emotionSadness = emotionSadness; }

    public BigDecimal getEmotionSurprise() { return emotionSurprise; }
    public void setEmotionSurprise(BigDecimal emotionSurprise) { this.emotionSurprise = emotionSurprise; }

    public BigDecimal getToxicity() { return toxicity; }
    public void setToxicity(BigDecimal toxicity) { this.toxicity = toxicity; }

    public String getNsfw() { return nsfw; }
    public void setNsfw(String nsfw) { this.nsfw = nsfw; }

    public String getFacultyCode() { return facultyCode; }
    public void setFacultyCode(String facultyCode) { this.facultyCode = facultyCode; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    // -------- helper ที่ไม่ให้ Hibernate แมป (ป้องกันคอลัมน์ซ้ำ) --------
    @Transient
    public LocalDateTime getAnalyzedAtValue() { return analyzedAt; }

    @Transient
    public void setAnalyzedAtValue(LocalDateTime value) { this.analyzedAt = value; }
}
