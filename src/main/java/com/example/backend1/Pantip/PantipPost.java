package com.example.backend1.Pantip;

import jakarta.persistence.*;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "pantip_post")
public class PantipPost {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;

    @Column(length = 500)
    private String url;

    // สำหรับ PostgreSQL ใช้ TEXT แทน LONGTEXT
    @Lob
    @Column(columnDefinition = "TEXT")
    private String preview;

    private String author;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String content;

    private String postTime;

    @OneToMany(mappedBy = "post", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<PantipComment> comments = new ArrayList<>();

    public PantipPost() {
    }

    public PantipPost(String title, String url, String preview) {
        this.title = title;
        this.url = url;
        this.preview = preview;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }

    public String getPreview() { return preview; }
    public void setPreview(String preview) { this.preview = preview; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public String getPostTime() { return postTime; }
    public void setPostTime(String postTime) { this.postTime = postTime; }

    public List<PantipComment> getComments() { return comments; }
    public void setComments(List<PantipComment> comments) { this.comments = comments; }
}
