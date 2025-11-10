package com.example.backend1.ingest.twitter;

import jakarta.persistence.*;

@Entity
@Table(name = "tweet")
public class Tweet {

    // ใช้ id จากตาราง tweet เป็นคีย์หลักตรง ๆ
    // แนะนำให้ใช้ String เพื่อไม่เสี่ยง overflow ของเลขยาวแบบ Snowflake
    @Id
    @Column(name = "id", length = 64, nullable = false)
    private String id;

    @Column(name = "author_id", length = 64)
    private String authorId;

    @Column(name = "created_at", length = 64)
    private String createdAt;     // เก็บเป็น String ให้ตรงกับโค้ดปัจจุบัน

    @Column(name = "text", length = 2000)
    private String text;

    @Column(name = "sentiment", length = 32)
    private String sentiment;

    public Tweet() {}

    // ---------- getters / setters ----------
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getAuthorId() { return authorId; }
    public void setAuthorId(String authorId) { this.authorId = authorId; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
}
