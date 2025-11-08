package com.example.backend1.Twitter.analysis;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "tweet_analysis",
        indexes = {
                @Index(name = "idx_tweet_analysis_tweet_id", columnList = "tweet_id"),
                @Index(name = "idx_tweet_analysis_sentiment", columnList = "sentiment_label"),
                @Index(name = "idx_tweet_analysis_nsfw", columnList = "nsfw_label")
        })
public class TweetAnalysis {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "tweet_id", nullable = false, length = 100)
    private String tweetId;

    @Column(name = "lang", length = 8)
    private String lang;

    @Column(name = "sentiment_label", length = 8)
    private String sentimentLabel;

    // ใช้ BigDecimal ให้แมปกับ DECIMAL(6,4) ได้ถูกต้อง
    @Column(name = "sentiment_score", precision = 6, scale = 4)
    private BigDecimal sentimentScore;

    @Column(name = "nsfw_label", length = 16)
    private String nsfwLabel;

    @Column(name = "nsfw_score", precision = 6, scale = 4)
    private BigDecimal nsfwScore;

    @Column(name = "toxicity_score", precision = 6, scale = 4)
    private BigDecimal toxicityScore;

    @Column(name = "hate_speech_score", precision = 6, scale = 4)
    private BigDecimal hateSpeechScore;

    @Column(name = "faculty", length = 64)
    private String faculty;

    @Column(name = "topics", columnDefinition = "TEXT")
    private String topicsJson;

    @Column(name = "confidence_overall", precision = 6, scale = 4)
    private BigDecimal confidenceOverall;

    @Column(name = "version")
    private Integer version;

    @Column(name = "analyzed_at")
    private LocalDateTime analyzedAt;

    public TweetAnalysis() {}

    // --- getters/setters ---
    public Long getId() { return id; }

    public String getTweetId() { return tweetId; }
    public void setTweetId(String tweetId) { this.tweetId = tweetId; }

    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }

    public String getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(String sentimentLabel) { this.sentimentLabel = sentimentLabel; }

    public BigDecimal getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(BigDecimal sentimentScore) { this.sentimentScore = sentimentScore; }

    public String getNsfwLabel() { return nsfwLabel; }
    public void setNsfwLabel(String nsfwLabel) { this.nsfwLabel = nsfwLabel; }

    public BigDecimal getNsfwScore() { return nsfwScore; }
    public void setNsfwScore(BigDecimal nsfwScore) { this.nsfwScore = nsfwScore; }

    public BigDecimal getToxicityScore() { return toxicityScore; }
    public void setToxicityScore(BigDecimal toxicityScore) { this.toxicityScore = toxicityScore; }

    public BigDecimal getHateSpeechScore() { return hateSpeechScore; }
    public void setHateSpeechScore(BigDecimal hateSpeechScore) { this.hateSpeechScore = hateSpeechScore; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getTopicsJson() { return topicsJson; }
    public void setTopicsJson(String topicsJson) { this.topicsJson = topicsJson; }

    public BigDecimal getConfidenceOverall() { return confidenceOverall; }
    public void setConfidenceOverall(BigDecimal confidenceOverall) { this.confidenceOverall = confidenceOverall; }

    public Integer getVersion() { return version; }
    public void setVersion(Integer version) { this.version = version; }

    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
