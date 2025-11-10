package com.example.backend1.ingest.pantip;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface PantipPostRepository extends JpaRepository<PantipPost, Long> {

    @Query(value = """
        SELECT p.* FROM pantip_post p
        LEFT JOIN typhoon_analysis ta
          ON ta.source_table = 'pantip_post'
         AND ta.source_id = CAST(p.id AS CHAR)
        WHERE ta.id IS NULL
        ORDER BY p.id DESC
        """, nativeQuery = true)
    List<PantipPost> findUnanalyzedPosts();
}
