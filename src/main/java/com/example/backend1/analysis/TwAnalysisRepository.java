package com.example.backend1.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface TwAnalysisRepository extends JpaRepository<TwAnalysis, Long> {

    @Query(value = """
        SELECT DATE_FORMAT(a.analyzed_at, '%Y-%m') AS month,
               COUNT(*) AS total,
               SUM(CASE WHEN a.sentiment='positive' THEN 1 ELSE 0 END) AS pos,
               SUM(CASE WHEN a.sentiment='neutral'  THEN 1 ELSE 0 END) AS neu,
               SUM(CASE WHEN a.sentiment='negative' THEN 1 ELSE 0 END) AS neg
        FROM twanalysis a
        WHERE (:app IS NULL OR a.app = :app)
          AND a.analyzed_at BETWEEN :from AND :to
        GROUP BY month
        ORDER BY month
        """, nativeQuery = true)
    List<Object[]> aggregateMonthly(@Param("app") String app,
                                    @Param("from") LocalDateTime from,
                                    @Param("to") LocalDateTime to);

    @Query(value = """
        SELECT a.sentiment, a.post_type, COUNT(*) AS cnt
        FROM twanalysis a
        WHERE (:app IS NULL OR a.app = :app)
          AND a.analyzed_at BETWEEN :from AND :to
        GROUP BY a.sentiment, a.post_type
        ORDER BY a.sentiment ASC, cnt DESC
        """, nativeQuery = true)
    List<Object[]> sentimentByPostType(@Param("app") String app,
                                       @Param("from") LocalDateTime from,
                                       @Param("to") LocalDateTime to);
}
