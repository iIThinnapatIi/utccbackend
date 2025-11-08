package com.example.backend1.analysis.dto;

import java.util.List;

public class MonthlyAggregateResponse {
    public record Row(String month, long total, long pos, long neu, long neg) {}
    private List<Row> byMonth;
    public List<Row> getByMonth() { return byMonth; }
    public void setByMonth(List<Row> byMonth) { this.byMonth = byMonth; }
}
