package com.example.backend1.sentiment.dto;

import lombok.Data;
import java.util.List;

@Data
public class BatchAnalyzeRequest {
    private List<String> texts;
    public BatchAnalyzeRequest() {}
    public BatchAnalyzeRequest(List<String> texts) { this.texts = texts; }
}
