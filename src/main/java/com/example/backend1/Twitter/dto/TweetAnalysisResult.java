package com.example.backend1.Twitter.dto;

import java.time.LocalDateTime;
import java.util.List;

public class TweetAnalysisResult {
    private String tweetId;
    private String lang;

    private String sentimentLabel;
    private double sentimentScore;

    private String nsfwLabel;
    private double nsfwScore;

    private double toxicityScore;
    private double hateSpeechScore;

    private String faculty;
    private List<String> topics;

    private double confidenceOverall;
    private int version;
    private LocalDateTime analyzedAt;

    public TweetAnalysisResult() {}

    // --- getters/setters ---
    public String getTweetId() { return tweetId; }
    public void setTweetId(String tweetId) { this.tweetId = tweetId; }
    public String getLang() { return lang; }
    public void setLang(String lang) { this.lang = lang; }
    public String getSentimentLabel() { return sentimentLabel; }
    public void setSentimentLabel(String sentimentLabel) { this.sentimentLabel = sentimentLabel; }
    public double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(double sentimentScore) { this.sentimentScore = sentimentScore; }
    public String getNsfwLabel() { return nsfwLabel; }
    public void setNsfwLabel(String nsfwLabel) { this.nsfwLabel = nsfwLabel; }
    public double getNsfwScore() { return nsfwScore; }
    public void setNsfwScore(double nsfwScore) { this.nsfwScore = nsfwScore; }
    public double getToxicityScore() { return toxicityScore; }
    public void setToxicityScore(double toxicityScore) { this.toxicityScore = toxicityScore; }
    public double getHateSpeechScore() { return hateSpeechScore; }
    public void setHateSpeechScore(double hateSpeechScore) { this.hateSpeechScore = hateSpeechScore; }
    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }
    public List<String> getTopics() { return topics; }
    public void setTopics(List<String> topics) { this.topics = topics; }
    public double getConfidenceOverall() { return confidenceOverall; }
    public void setConfidenceOverall(double confidenceOverall) { this.confidenceOverall = confidenceOverall; }
    public int getVersion() { return version; }
    public void setVersion(int version) { this.version = version; }
    public LocalDateTime getAnalyzedAt() { return analyzedAt; }
    public void setAnalyzedAt(LocalDateTime analyzedAt) { this.analyzedAt = analyzedAt; }
}
