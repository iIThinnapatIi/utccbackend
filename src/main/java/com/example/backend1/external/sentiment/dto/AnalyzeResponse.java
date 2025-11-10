package com.example.backend1.external.sentiment.dto;

public class AnalyzeResponse {
    private String label;   // positive / neutral / negative
    private double score;   // 0..1
    private String raw;     // เก็บ raw json/ข้อความ

    public String getLabel() { return label; }
    public void setLabel(String label) { this.label = label; }

    public double getScore() { return score; }
    public void setScore(double score) { this.score = score; }

    public String getRaw() { return raw; }
    public void setRaw(String raw) { this.raw = raw; }
}
