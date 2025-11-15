package com.example.backend1.analysis.dto;

public class AnalyzeResponse {

    // ฟิลด์หลัก
    private String sentiment;          // positive | neutral | negative
    private String topic;              // หัวข้อหลัก
    private String answerRaw;          // JSON ดิบที่ LLM ส่งกลับมา

    // คะแนน + เหตุผล
    private Integer sentimentScore;        // 0–100
    private String rationaleSentiment;     // เหตุผลว่าทำไมได้คะแนนนี้
    private String rationaleIntent;        // เหตุผลว่าทำไมมองว่าเป็น intent นี้

    // ฟิลด์เชิงลึกอื่น ๆ
    private String intent;             // question | complaint | praise | ...
    private String utccRelevance;      // high | medium | low | none
    private String emotion;            // angry | sad | happy | worried | neutral | ...
    private String impactLevel;        // impact_low | impact_medium | impact_high
    private String nsfw;               // safe | borderline | nsfw
    private String toxicity;           // none | low | medium | high
    private String actor;              // prospective_student | current_student | ...
    private String hiddenMeaning;      // ประชด / กังวล / แฝงเชิงลบ ฯลฯ

    // faculty_guess ทั้งก้อนเก็บเป็น JSON string
    private String facultyGuessJson;

    // ---------- getters / setters ----------

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getAnswerRaw() {
        return answerRaw;
    }

    public void setAnswerRaw(String answerRaw) {
        this.answerRaw = answerRaw;
    }

    public Integer getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(Integer sentimentScore) {
        this.sentimentScore = sentimentScore;
    }

    public String getRationaleSentiment() {
        return rationaleSentiment;
    }

    public void setRationaleSentiment(String rationaleSentiment) {
        this.rationaleSentiment = rationaleSentiment;
    }

    public String getRationaleIntent() {
        return rationaleIntent;
    }

    public void setRationaleIntent(String rationaleIntent) {
        this.rationaleIntent = rationaleIntent;
    }

    public String getIntent() {
        return intent;
    }

    public void setIntent(String intent) {
        this.intent = intent;
    }

    public String getUtccRelevance() {
        return utccRelevance;
    }

    public void setUtccRelevance(String utccRelevance) {
        this.utccRelevance = utccRelevance;
    }

    public String getEmotion() {
        return emotion;
    }

    public void setEmotion(String emotion) {
        this.emotion = emotion;
    }

    public String getImpactLevel() {
        return impactLevel;
    }

    public void setImpactLevel(String impactLevel) {
        this.impactLevel = impactLevel;
    }

    public String getNsfw() {
        return nsfw;
    }

    public void setNsfw(String nsfw) {
        this.nsfw = nsfw;
    }

    public String getToxicity() {
        return toxicity;
    }

    public void setToxicity(String toxicity) {
        this.toxicity = toxicity;
    }

    public String getActor() {
        return actor;
    }

    public void setActor(String actor) {
        this.actor = actor;
    }

    public String getHiddenMeaning() {
        return hiddenMeaning;
    }

    public void setHiddenMeaning(String hiddenMeaning) {
        this.hiddenMeaning = hiddenMeaning;
    }

    public String getFacultyGuessJson() {
        return facultyGuessJson;
    }

    public void setFacultyGuessJson(String facultyGuessJson) {
        this.facultyGuessJson = facultyGuessJson;
    }
}
