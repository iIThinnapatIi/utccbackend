package com.example.backend1.analysis.repo;

import com.example.backend1.analysis.model.LlmAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LlmAnalysisRepository extends JpaRepository<LlmAnalysis, Long> {
}
