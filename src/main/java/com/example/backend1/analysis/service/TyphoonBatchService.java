package com.example.backend1.analysis.service;

import com.example.backend1.analysis.dto.AnalyzeRequest;
import com.example.backend1.analysis.dto.AnalyzeResponse;
import com.example.backend1.analysis.model.TyphoonAnalysis;
import com.example.backend1.analysis.repo.TyphoonAnalysisRepository;
import com.example.backend1.ingest.pantip.PantipPost;
import com.example.backend1.ingest.pantip.PantipPostRepository;
import com.example.backend1.ingest.twitter.Tweet;
import com.example.backend1.ingest.twitter.TweetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class TyphoonBatchService {

    private final LlmAnalyzer llmAnalyzer;
    private final TweetRepository tweetRepo;
    private final PantipPostRepository pantipRepo;
    private final TyphoonAnalysisRepository typhoonRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public TyphoonBatchService(
            LlmAnalyzer llmAnalyzer,
            TweetRepository tweetRepo,
            PantipPostRepository pantipRepo,
            TyphoonAnalysisRepository typhoonRepo
    ) {
        this.llmAnalyzer = llmAnalyzer;
        this.tweetRepo = tweetRepo;
        this.pantipRepo = pantipRepo;
        this.typhoonRepo = typhoonRepo;
    }

    @Transactional
    public int analyzeTweets() {
        int inserted = 0;
        List<Tweet> tweets = tweetRepo.findUnanalyzedTweets();
        for (Tweet t : tweets) {
            String text = nvl(t.getText());
            if (text.isBlank()) continue;

            AnalyzeRequest req = new AnalyzeRequest();
            req.setText(text);
            req.setApp("web");
            req.setSource("twitter");
            req.setModel("qwen2.5:7b-instruct");
            req.setTemperature(0.7);
            req.setMaxTokens(256);
            req.setSave(false);

            AnalyzeResponse res;
            try {
                res = llmAnalyzer.analyze(req);
            } catch (Exception e) {
                System.err.println("❌ Error analyzing tweet ID " + t.getId() + ": " + e.getMessage());
                continue; // ข้ามไป tweet ถัดไป
            }

            // ป้องกันซ้ำตาม source_table + source_id
            String sourceTable = "tweet";
            String sourceId = String.valueOf(t.getId());
            if (typhoonRepo.existsBySourceTableAndSourceId(sourceTable, sourceId)) {
                continue;
            }

            TyphoonAnalysis row = new TyphoonAnalysis();
            row.setApp("web");
            row.setSourceTable(sourceTable);
            row.setSourceId(sourceId);
            row.setPostType("post");
            row.setLanguage("th");

            row.setSentiment(nullIfEmpty(res.getSentiment()));
            row.setTopic(nullIfEmpty(res.getTopic()));
            row.setSummary(extractSummaryOrFallback(res.getAnswerRaw(), text));

            // created_at ใช้เวลาต้นฉบับถ้ามี ไม่งั้น now
            LocalDateTime createdAt = parseTimeOrNull(safeTweetCreatedAt(t));
            row.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());

            // analyzed_at ใช้เมธอด non-JavaBean เพื่อหลบการแมปซ้ำ
            row.setAnalyzedAtValue(LocalDateTime.now());

            typhoonRepo.save(row);

            // อัปเดตสถานะในตารางต้นทางตามที่คุณใช้งานอยู่
            t.setSentiment(res.getSentiment());
            tweetRepo.save(t);

            inserted++;
        }
        return inserted;
    }

    @Transactional
    public int analyzePantip() {
        int inserted = 0;
        List<PantipPost> posts = pantipRepo.findUnanalyzedPosts();

        for (PantipPost p : posts) {
            String base = firstNonBlank(nvl(p.getContent()), nvl(p.getPreview()), nvl(p.getTitle()));
            if (base.isBlank()) continue;

            AnalyzeRequest req = new AnalyzeRequest();
            req.setText(base);
            req.setApp("web");
            req.setSource("pantip");
            req.setModel("qwen2.5:7b-instruct");
            req.setTemperature(0.7);
            req.setMaxTokens(256);
            req.setSave(false);

            AnalyzeResponse res;
            try {
                res = llmAnalyzer.analyze(req);
            } catch (Exception e) {
                System.err.printf("❌ Error analyzing pantip post ID %s: %s%n", p.getId(), e.getMessage());
                continue;
            }

            String sourceTable = "pantip_post";
            String sourceId = String.valueOf(p.getId());
            if (typhoonRepo.existsBySourceTableAndSourceId(sourceTable, sourceId)) {
                continue;
            }

            TyphoonAnalysis row = new TyphoonAnalysis();
            row.setApp("web");
            row.setSourceTable(sourceTable);
            row.setSourceId(sourceId);
            row.setPostType("post");
            row.setLanguage("th");
            row.setSentiment(nullIfEmpty(res.getSentiment()));
            row.setTopic(nullIfEmpty(res.getTopic()));
            row.setSummary(extractSummaryOrFallback(res.getAnswerRaw(), base));
            row.setCreatedAt(java.time.LocalDateTime.now());
            row.setAnalyzedAtValue(LocalDateTime.now());

            typhoonRepo.save(row);

            // อัปเดตสถานะในต้นทาง
            p.setSentiment(res.getSentiment());
            pantipRepo.save(p);

            inserted++;
        }
        return inserted;
    }



    // -------- utils --------
    private String extractSummaryOrFallback(String answerRaw, String fallback) {
        if (answerRaw == null || answerRaw.isBlank()) return cut(fallback);
        try {
            JsonNode n = mapper.readTree(answerRaw);
            String s = n.path("summary").asText("");
            return s.isBlank() ? cut(fallback) : s;
        } catch (Exception ignore) {
            return cut(fallback);
        }
    }

    private String safeTweetCreatedAt(Tweet t) {
        try { return String.valueOf(t.getCreatedAt()); }
        catch (Exception e) { return null; }
    }

    private LocalDateTime parseTimeOrNull(String createdAtStr) {
        if (createdAtStr == null || createdAtStr.isBlank()) return null;
        try {
            // รองรับรูปแบบ "yyyy-MM-dd HH:mm:ss"
            return LocalDateTime.parse(createdAtStr.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) if (v != null && !v.isBlank()) return v;
        return "";
    }

    private String cut(String s) {
        String v = nvl(s);
        return v.length() > 200 ? v.substring(0, 200) : v;
    }

    private String nvl(String s) { return s == null ? "" : s; }

    private String nullIfEmpty(String s) { return (s == null || s.isBlank()) ? null : s; }
}
