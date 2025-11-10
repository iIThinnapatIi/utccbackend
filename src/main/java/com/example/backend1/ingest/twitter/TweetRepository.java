package com.example.backend1.ingest.twitter;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import java.util.List;

public interface TweetRepository extends JpaRepository<Tweet, Long> {

    // ใช้ native SQL เพื่อตัดทิ้งรายการที่ไปอยู่ใน typhoon_analysis แล้ว
    @Query(value = """
        SELECT t.* FROM tweet t
        LEFT JOIN typhoon_analysis ta
          ON ta.source_table = 'tweet'
         AND ta.source_id = CAST(t.id AS CHAR)
        WHERE ta.id IS NULL
        ORDER BY t.created_at DESC
        """, nativeQuery = true)
    List<Tweet> findUnanalyzedTweets();
}
