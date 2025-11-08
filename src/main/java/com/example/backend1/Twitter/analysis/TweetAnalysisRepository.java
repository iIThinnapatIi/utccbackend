package com.example.backend1.Twitter.analysis;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Repository สำหรับเข้าถึงตาราง tweet_analysis
 * ใช้กับ Entity TweetAnalysis โดย Spring Data JPA จะสร้างเมธอดพื้นฐานให้อัตโนมัติ
 */
@Repository
public interface TweetAnalysisRepository extends JpaRepository<TweetAnalysis, Long> {

    /**
     * ค้นหาผลการวิเคราะห์จาก tweetId
     * เช่น ใช้ตอนต้องการดูผลเฉพาะโพสต์
     */
    Optional<TweetAnalysis> findByTweetId(String tweetId);

    /**
     * ดึงรายการวิเคราะห์ทั้งหมดที่ sentimentLabel ตรงตามที่ระบุ (pos, neu, neg)
     */
    List<TweetAnalysis> findBySentimentLabelIgnoreCase(String sentimentLabel);

    /**
     * ดึงรายการทั้งหมดตามคณะ (faculty)
     */
    List<TweetAnalysis> findByFacultyIgnoreCase(String faculty);
}
