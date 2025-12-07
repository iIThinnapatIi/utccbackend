package com.example.backend1.Analysis;

import com.example.backend1.Faculty.Faculty;
import com.example.backend1.Pantip.PantipComment;
import com.example.backend1.Pantip.PantipPost;
import com.example.backend1.Twitter.Tweet;
import jakarta.persistence.*;
import lombok.Data;

/**
 * Entity นี้แมปกับตาราง social_analysis ใน DB
 * ใช้เก็บผลการวิเคราะห์แต่ละโพสต์/ทวีต
 */
@Entity
@Table(name = "social_analysis")
@Data // จาก Lombok: auto-gen getter/setter, toString(), equals(), hashCode()
public class Analysis {

    /**
     * ใช้เก็บไอดีของโพสต์/ทวีต
     * แมปกับคอลัมน์ id (PRIMARY KEY) ใน social_analysis
     */
    @Id
    private String id;

    /**
     * เนื้อหาข้อความจริง ๆ ที่ดึงมาจากแพลตฟอร์ม (โพสต์ยาว)
     * - @Lob บอก Hibernate ให้เก็บแบบ Large Object
     * - columnDefinition = "TEXT"
     *   PostgreSQL รองรับ TEXT เก็บข้อความยาวได้ถึง 1GB
     *
     * ใช้แทน LONGTEXT (เพราะ PostgreSQL ไม่มี LONGTEXT)
     */
    @Lob
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    /**
     * เวลาที่โพสต์ถูกสร้าง (string ที่ parse มาแล้ว)
     * แมปกับคอลัมน์ created_at
     */
    @Column(name = "created_at")
    private String createdAt;

    /**
     * แพลตฟอร์มที่มาของโพสต์ เช่น twitter, pantip
     * แมปกับคอลัมน์ platform
     */
    private String platform;

    /**
     * คณะที่โมเดล/กฎของเราตีความ เช่น การตลาด, นิเทศฯ ฯลฯ
     * แมปกับคอลัมน์ faculty
     */
    private String faculty;

    /**
     * label อารมณ์รวมของโพสต์ เช่น positive / neutral / negative
     * แมปกับคอลัมน์ sentiment
     */
    private String sentiment;

    /**
     * label สุดท้ายที่ใช้แสดงบน dashboard
     * เช่น รวม faculty + sentiment
     * แมปกับคอลัมน์ final_label
     */
    @Column(name = "final_label")
    private String finalLabel;

    /**
     * ความสัมพันธ์กับ Tweet (nullable ได้)
     */
    @ManyToOne
    @JoinColumn(name = "tweet_id", nullable = true)
    private Tweet tweet;

    /**
     * ความสัมพันธ์แบบ ManyToOne กับ PantipPost
     */
    @ManyToOne
    @JoinColumn(name = "pantip_post_id", nullable = true)
    private PantipPost pantipPost;

    /**
     * ความสัมพันธ์แบบ ManyToOne กับ PantipComment
     */
    @ManyToOne
    @JoinColumn(name = "pantip_comment_id", nullable = true)
    private PantipComment pantipComment;

    // =====================================================
    // ⭐ ใหม่: FK ไปหา table faculty (faculty_id)
    // =====================================================
    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = true)
    private Faculty facultyRef;

    /**
     * ความมั่นใจของโมเดล (probability 0–1)
     * แมปกับคอลัมน์ sentiment_score
     */
    @Column(name = "sentiment_score")
    private Double sentimentScore;
}
