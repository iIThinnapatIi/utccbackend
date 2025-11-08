package com.example.backend1.analysis.dto;

import java.util.List;

public class SentimentPostTypeResponse {
    public record Row(String sentiment, String postType, long count) {}
    private List<Row> matrix;
    public List<Row> getMatrix() { return matrix; }
    public void setMatrix(List<Row> matrix) { this.matrix = matrix; }
}
