package com.example.backend1.analysis.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "llm_analysis")
public class LlmAnalysis {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String app;
    private String source;

    @Column(columnDefinition = "TEXT")
    private String text;

    private String sentiment;
    private String topic;

    @Column(columnDefinition = "TEXT")
    private String answerRaw;

    private LocalDateTime createdAt;

    // getters/setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getAnswerRaw() { return answerRaw; }
    public void setAnswerRaw(String answerRaw) { this.answerRaw = answerRaw; }

    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
}
