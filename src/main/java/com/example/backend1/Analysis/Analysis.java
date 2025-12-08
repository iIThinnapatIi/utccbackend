package com.example.backend1.Analysis;

import com.example.backend1.Faculty.Faculty;
import com.example.backend1.Pantip.PantipComment;
import com.example.backend1.Pantip.PantipPost;
import com.example.backend1.Twitter.Tweet;
import jakarta.persistence.*;

@Entity
@Table(name = "social_analysis")
public class Analysis {

    @Id
    private String id;

    // ✅ เอา @Lob ออก เหลือ TEXT เฉย ๆ
    @Column(name = "text", columnDefinition = "TEXT")
    private String text;

    @Column(name = "created_at")
    private String createdAt;

    private String platform;

    private String faculty;

    private String sentiment;

    @Column(name = "final_label")
    private String finalLabel;

    @ManyToOne
    @JoinColumn(name = "tweet_id", nullable = true)
    private Tweet tweet;

    @ManyToOne
    @JoinColumn(name = "pantip_post_id", nullable = true)
    private PantipPost pantipPost;

    @ManyToOne
    @JoinColumn(name = "pantip_comment_id", nullable = true)
    private PantipComment pantipComment;

    @ManyToOne
    @JoinColumn(name = "faculty_id", nullable = true)
    private Faculty facultyRef;

    @Column(name = "sentiment_score")
    private Double sentimentScore;

    public Analysis() {}

    // --- getters / setters เหมือนเดิม ---
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getCreatedAt() { return createdAt; }
    public void setCreatedAt(String createdAt) { this.createdAt = createdAt; }

    public String getPlatform() { return platform; }
    public void setPlatform(String platform) { this.platform = platform; }

    public String getFaculty() { return faculty; }
    public void setFaculty(String faculty) { this.faculty = faculty; }

    public String getSentiment() { return sentiment; }
    public void setSentiment(String sentiment) { this.sentiment = sentiment; }

    public String getFinalLabel() { return finalLabel; }
    public void setFinalLabel(String finalLabel) { this.finalLabel = finalLabel; }

    public Tweet getTweet() { return tweet; }
    public void setTweet(Tweet tweet) { this.tweet = tweet; }

    public PantipPost getPantipPost() { return pantipPost; }
    public void setPantipPost(PantipPost pantipPost) { this.pantipPost = pantipPost; }

    public PantipComment getPantipComment() { return pantipComment; }
    public void setPantipComment(PantipComment pantipComment) { this.pantipComment = pantipComment; }

    public Faculty getFacultyRef() { return facultyRef; }
    public void setFacultyRef(Faculty facultyRef) { this.facultyRef = facultyRef; }

    public Double getSentimentScore() { return sentimentScore; }
    public void setSentimentScore(Double sentimentScore) { this.sentimentScore = sentimentScore; }
}
