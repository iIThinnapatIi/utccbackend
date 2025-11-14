package com.example.backend1.analysis.dto;

public class AnalyzeResponse {

    private String sentiment;          // positive | neutral | negative
    private String topic;              // หัวข้อ (คณะ / ระบบ / กิจกรรม ฯลฯ)
    private String answerRaw;          // เก็บ JSON ดิบที่ LLM ส่งกลับมา

    // -------- ฟิลด์ใหม่ที่รองรับระบบวิเคราะห์เวอร์ชันลึก --------

    private String intent;             // question | complaint | praise | share_experience | information | promotion | spam_or_irrelevant | other
    private String utccRelevance;      // high | medium | low | none
    private String nsfw;               // safe | mild | adult
    private String toxicity;           // none | low | medium | high
    private String actor;              // prospective_student | current_student | alumni | parent | staff_or_teacher | general_public | business | unknown
    private String hiddenMeaning;      // ประชด / กังวล / เสียดสี / แฝงเชิงลบ ฯลฯ

    // เก็บ faculty_guess ทั้ง object JSON
    private String facultyGuessJson;

    // ---------------------------------------------------------------

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

    public void setEmotion(String neutral) {
    }

    public void setImpactLevel(String impactLow) {
    }
}
