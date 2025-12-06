package com.example.backend1.Analysis;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.Objects;

@Embeddable
public class AnalysisCustomKeywordId implements Serializable {

    private String analysisId;
    private Long keywordId;

    public AnalysisCustomKeywordId() {}

    public AnalysisCustomKeywordId(String analysisId, Long keywordId) {
        this.analysisId = analysisId;
        this.keywordId = keywordId;
    }

    // equals & hashcode
    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AnalysisCustomKeywordId)) return false;
        AnalysisCustomKeywordId that = (AnalysisCustomKeywordId) o;
        return Objects.equals(analysisId, that.analysisId) &&
                Objects.equals(keywordId, that.keywordId);
    }

    @Override
    public int hashCode() {
        return Objects.hash(analysisId, keywordId);
    }
}
