package com.example.backend1.Analysis;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AnalysisCustomKeywordRepo
        extends JpaRepository<AnalysisCustomKeyword, AnalysisCustomKeywordId> {
}
