// src/main/java/com/example/backend1/analysis/repo/AnalysisViewRepository.java
package com.example.backend1.analysis.repo;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;

@Repository
public interface AnalysisViewRepository extends JpaRepository<com.example.backend1.analysis.model.TyphoonAnalysis, Long> {

    // 1) summary
    @Query(value = "SELECT * FROM v_sentiment_overview", nativeQuery = true)
    List<Map<String, Object>> sentimentOverview();

    // 2) trend daily
    @Query(value = "SELECT * FROM v_trend_daily ORDER BY day", nativeQuery = true)
    List<Map<String, Object>> trendDaily();

    // 3) top faculties
    @Query(value = "SELECT * FROM v_faculty_summary ORDER BY total DESC LIMIT 10", nativeQuery = true)
    List<Map<String, Object>> topFaculties();

    // 4) latest mentions (ให้ frontend ส่ง page,size มาก็ได้ หรือเอา default)
    @Query(value = "SELECT * FROM v_latest_mentions ORDER BY created_at DESC LIMIT ?2 OFFSET ?1", nativeQuery = true)
    List<Map<String, Object>> latestMentions(int offset, int limit);

    @Query(value = "SELECT COUNT(1) FROM v_latest_mentions", nativeQuery = true)
    long latestMentionsCount();
}
