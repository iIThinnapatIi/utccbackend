package com.example.backend1.model;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "posts")
public class Post {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private Source source;

    private String extId;
    private String author;

    @Column(columnDefinition = "TEXT")
    private String textRaw;

    @Column(columnDefinition = "TEXT")
    private String textNorm;

    private String url;

    private LocalDateTime createdAt;
    private Boolean hasMedia = false;

    @Enumerated(EnumType.STRING)
    private com.example.backend1.model.Sentiment sentiment = com.example.backend1.model.Sentiment.neutral;

    private BigDecimal relevance = BigDecimal.ZERO;
    private String lang;      // "th" | "en"
    private String textHash;  // SHA1(textNorm)
    private LocalDateTime createdAtIngested = LocalDateTime.now();

    // getters & setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Source getSource() { return source; }
    public void setSource(Source source) { this.source = source; }
    public String getExtId() { return extId; }
    public void setExtId(String extId) { this.extId = extId; }
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    public String getTextRaw() { return textRaw; }
    public void setTextRaw(String textRaw) { this.textRaw = textRaw; }
    public String getTextNorm() { return textNorm; }
    public void setTextNorm(String textNorm) { this.textNorm = textNorm; }
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public Boolean getHasMedia() { return hasMedia; }
    public void setHasMedia(Boolean hasMedia) { this.hasMedia = hasMedia; }
    public com.example.backend1.model.Sentiment getSentiment() { return sentiment; }
    public void setSentiment(com.example.backend1.model.Sentiment sentiment) { this.sentiment = sentiment; }
    public BigDecimal getRelevance() { return relevance; }
    public void setRelevance(BigDecimal relevance) { this.relevance = relevance; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getTextHash() { return textHash; }
    public void setTextHash(String textHash) { this.textHash = textHash; }
    public LocalDateTime getCreatedAtIngested() { return createdAtIngested; }
    public void setCreatedAtIngested(LocalDateTime createdAtIngested) { this.createdAtIngested = createdAtIngested; }
}