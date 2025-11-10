package com.example.backend1.ingest.pantip;

import jakarta.persistence.*;

@Entity
@Table(name = "pantip_post")
public class PantipPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(length = 500)
    private String title;

    @Column(length = 1000)
    private String url;

    @Column(length = 2000)
    private String preview;

    @Column(length = 8000)
    private String content;

    @Column(length = 32)
    private String sentiment;

    public PantipPost() {}

    // คอนสตรัคเตอร์ 3 พารามิเตอร์ สำหรับโค้ดที่ new PantipPost(title, url, preview)
    public PantipPost(String title, String url, String preview) {
        this.title = title;
        this.url = url;
        this.preview = preview;
    }

    // -------- getters/setters ชัดเจน --------
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }
}
