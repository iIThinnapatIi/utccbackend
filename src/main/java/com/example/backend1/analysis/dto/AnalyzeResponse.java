package com.example.backend1.analysis.dto;

public class AnalyzeResponse {
    private String sentiment;
    private String topic;
    private String answerRaw;

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public String getTopic() { return topic; }
    public void setTopic(String topic) { this.topic = topic; }

    public String getAnswerRaw() { return answerRaw; }
    public void setAnswerRaw(String answerRaw) { this.answerRaw = answerRaw; }
}
