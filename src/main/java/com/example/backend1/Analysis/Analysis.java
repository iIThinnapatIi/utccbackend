package com.example.backend1.Analysis;

import com.example.backend1.Faculty.Faculty;
import com.example.backend1.Pantip.PantipComment;
import com.example.backend1.Pantip.PantipPost;
import com.example.backend1.Twitter.Tweet;
import jakarta.persistence.*;

/**
 * Entity นี้แมปกับตาราง social_analysis ในฐานข้อมูล
 * ใช้เก็บผลการวิเคราะห์แต่ละโพสต์/ทวีต/คอมเมนต์
 */
@Entity
@Table(name = "social_analysis")
public class Analysis {

    /** ไอดีของโพสต์/ทวีต/คอมเมนต์ ในตาราง social_analysis */
    @Id
    private String id;

    /**
     * เนื้อหาข้อความจริง ๆ ที่ดึงมาจากแพลตฟอร์ม (โพสต์ยาว)
     * ใช้ TEXT เพื่อรองรับข้อความยาว
     * ✅ เอา @Lob ออกเพื่อไม่ให้ชนกับ PostgreSQL (Bad value for type long)
     */
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    /** เวลาโพสต์ / เวลาที่เราเก็บข้อมูล */
    @Column(name = "created_at")
    private String createdAt;

    /** แพลตฟอร์มที่มาของโพสต์ เช่น twitter, pantip_post, pantip_comment */
    private String platform;

    /** ชื่อคณะที่ระบบตีความได้ เช่น การตลาด นิเทศฯ ฯลฯ */
    private String faculty;

    /** sentiment หลักของข้อความ เช่น positive / neutral / negative */
    private String sentiment;

    /** label สุดท้ายที่ใช้แสดงบน dashboard (หลังผู้ใช้แก้ไขแล้วก็ได้) */
    @Column(name = "final_label")
    private String finalLabel;

    /* -------------------- ความสัมพันธ์กับตารางอื่น -------------------- */

    /** ถ้าเป็นข้อมูลจาก Twitter จะอ้างถึง tweet_id */
    @ManyToOne
    @JoinColumn(name = "tweet_id", nullable = true)
    private Tweet tweet;

    /** ถ้าเป็นข้อมูลจาก Pantip post → pantip_post_id */
    @ManyToOne
    @JoinColumn(name = "pantip_post_id", nullable = true)
    private PantipPost pantipPost;

    /** ถ้าเป็นข้อมูลจาก Pantip comment → pantip_comment_id */
    @ManyToOne
    @JoinColumn(name = "pantip_comment_id", nullable = true)
    private PantipComment pantipComment;

    /** FK ไปหาตาราง faculty (ใช้ตอนอยาก join ดูข้อมูลคณะละเอียด) */
    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = true)
    private Faculty facultyRef;

    /** ความมั่นใจของโมเดล 0–1 */
    @Column(name = "sentiment_score")
    private Double sentimentScore;

    /* ======================  Constructor  ====================== */

    public Analysis() {
    }

    /* ======================  Getter / Setter  ====================== */

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getText() {
        return text;
    }

    public void setText(String text) {
        this.text = text;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getPlatform() {
        return platform;
    }

    public void setPlatform(String platform) {
        this.platform = platform;
    }

    public String getFaculty() {
        return faculty;
    }

    public void setFaculty(String faculty) {
        this.faculty = faculty;
    }

    public String getSentiment() {
        return sentiment;
    }

    public void setSentiment(String sentiment) {
        this.sentiment = sentiment;
    }

    public String getFinalLabel() {
        return finalLabel;
    }

    public void setFinalLabel(String finalLabel) {
        this.finalLabel = finalLabel;
    }

    public Tweet getTweet() {
        return tweet;
    }

    public void setTweet(Tweet tweet) {
        this.tweet = tweet;
    }

    public PantipPost getPantipPost() {
        return pantipPost;
    }

    public void setPantipPost(PantipPost pantipPost) {
        this.pantipPost = pantipPost;
    }

    public PantipComment getPantipComment() {
        return pantipComment;
    }

    public void setPantipComment(PantipComment pantipComment) {
        this.pantipComment = pantipComment;
    }

    public Faculty getFacultyRef() {
        return facultyRef;
    }

    public void setFacultyRef(Faculty facultyRef) {
        this.facultyRef = facultyRef;
    }

    public Double getSentimentScore() {
        return sentimentScore;
    }

    public void setSentimentScore(Double sentimentScore) {
        this.sentimentScore = sentimentScore;
    }
}
