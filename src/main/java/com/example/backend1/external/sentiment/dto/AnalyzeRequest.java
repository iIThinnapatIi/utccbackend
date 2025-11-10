package com.example.backend1.external.sentiment.dto;

import lombok.Data;

@Data
public class AnalyzeRequest {
    private String text;
    public AnalyzeRequest() {}
    public AnalyzeRequest(String text) { this.text = text; }
}
