package com.example.backend1.model;


import java.time.OffsetDateTime;
import java.util.Set;


public class AnalyzedTweet {
    public String id;
    public String text;
    public String authorId;
    public OffsetDateTime createdAt;
    public String sentiment; // positive | negative | neutral
    public boolean adultFlag; // true if contains 18+ content
    public Set<String> faculties; // matched faculties
    public Set<String> categories; // e.g., ["UTCC", "18+", "sent:positive"]
}