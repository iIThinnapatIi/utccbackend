package com.example.backend1.Pantip;

import jakarta.persistence.*;

@Entity
@Table(name = "pantip_comment")
public class PantipComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Lob
    @Column(columnDefinition = "TEXT")   // <-- เดิม LONGTEXT
    private String text;

    private String author;

    private String commentedAt;

    @ManyToOne
    @JoinColumn(name = "post_id")
    private PantipPost post;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }

    public String getCommentedAt() { return commentedAt; }
    public void setCommentedAt(String commentedAt) { this.commentedAt = commentedAt; }

    public PantipPost getPost() { return post; }
    public void setPost(PantipPost post) { this.post = post; }
}
