package com.example.backend1.Analysis;

import jakarta.persistence.*;

@Entity
@Table(name = "analysis_custom_keyword")
public class AnalysisCustomKeyword {

    @EmbeddedId
    private AnalysisCustomKeywordId id;

    public AnalysisCustomKeyword() {}

    public AnalysisCustomKeyword(String analysisId, Long keywordId) {
        this.id = new AnalysisCustomKeywordId(analysisId, keywordId);
    }

    public AnalysisCustomKeywordId getId() {
        return id;
    }

    public void setId(AnalysisCustomKeywordId id) {
        this.id = id;
    }
}
