package com.example.backend1.analysis.repo;

import com.example.backend1.analysis.model.TyphoonAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TyphoonAnalysisRepository extends JpaRepository<TyphoonAnalysis, Long> {
    boolean existsBySourceTableAndSourceId(String sourceTable, String sourceId);

}



