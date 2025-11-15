package com.example.backend1.analysis.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "typhoon_analysis")
@Access(AccessType.FIELD) // ให้ Hibernate แมปจาก field names → column names (snake_case)
public class TyphoonAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // แอปต้นทาง เช่น "tweet", "pantip_post"
    // column: app
    private String app;

    // ตารางต้นทาง + id ต้นทาง
    // column: source_table, source_id
    private String sourceTable;
    private String sourceId;

    // ประเภทโพสต์ เช่น post / comment
    // column: post_type
    private String postType;

    // ภาษา เช่น "th"
    private String language;

    // sentiment = positive / neutral / negative
    private String sentiment;

    // คะแนน sentiment 0–100 (เก็บเป็นคอลัมน์ sentiment_score)
    @Column(name = "sentiment_score", precision = 5, scale = 2)
    private BigDecimal sentimentScore;

    // topic แบบสั้น ๆ เช่น admission / tuition_and_loan
    private String topic;

    // JSON ละเอียดของการวิเคราะห์ (intent, emotion, impact ฯลฯ)
    // column: topics_json
    @Column(columnDefinition = "TEXT")
    private String topicsJson;

    // สรุปโพสต์ภาษาไทย 1–2 ประโยค
    @Column(columnDefinition = "TEXT")
    private String summary;

    // ระดับ toxicity / nsfw สรุป
    private String toxicity;
    private String nsfw;

    // เดาคณะ / faculty_code เช่น BUA, ACC, COMM ฯลฯ
    private String facultyCode;

    // เหตุผลที่ให้ sentiment นี้ (ภาษาไทยยาว ๆ)
    @Column(name = "rationale_sentiment", columnDefinition = "TEXT")
    private String rationaleSentiment;

    // เหตุผลเรื่อง intent/เจตนา ของโพสต์
    @Column(name = "rationale_intent", columnDefinition = "TEXT")
    private String rationaleIntent;

    // เวลาต้นทางของโพสต์ (หรือเวลาที่บันทึกครั้งแรก)
    private LocalDateTime createdAt;

    // เวลา analyzed_at ที่ใช้เรียงลำดับ batch
    private LocalDateTime analyzedAt;

    // ----------------------------------------------------
    // getters / setters
    // ----------------------------------------------------

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

    public String getToxicity() { return toxicity; }
    public void setToxicity(String toxicity) { this.toxicity = toxicity; }

    public String getNsfw() { return nsfw; }
    public void setNsfw(String nsfw) { this.nsfw = nsfw; }

    public String getFacultyCode() { return facultyCode; }
    public void setFacultyCode(String facultyCode) { this.facultyCode = facultyCode; }

    public String getRationaleSentiment() { return rationaleSentiment; }
    public void setRationaleSentiment(String rationaleSentiment) { this.rationaleSentiment = rationaleSentiment; }

    public String getRationaleIntent() { return rationaleIntent; }
    public void setRationaleIntent(String rationaleIntent) { this.rationaleIntent = rationaleIntent; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }

    // -------- helper ที่ TyphoonBatchService ใช้ (ไม่ให้ Hibernate แมปซ้ำ) --------
    @Transient
    public LocalDateTime getAnalyzedAtValue() { return analyzedAt; }

    @Transient
    public void setAnalyzedAtValue(LocalDateTime value) { this.analyzedAt = value; }
}
