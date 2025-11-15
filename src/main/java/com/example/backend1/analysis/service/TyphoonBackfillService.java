package com.example.backend1.analysis.service;

import com.example.backend1.analysis.dto.AnalyzeRequest;
import com.example.backend1.analysis.dto.AnalyzeResponse;
import com.example.backend1.analysis.model.TyphoonAnalysis;
import com.example.backend1.analysis.repo.TyphoonAnalysisRepository;
import com.example.backend1.ingest.twitter.Tweet;
import com.example.backend1.ingest.twitter.TweetRepository;
import com.example.backend1.ingest.pantip.PantipPost;
import com.example.backend1.ingest.pantip.PantipPostRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Service
public class TyphoonBackfillService {

    private final LlmAnalyzer analyzer;
    private final TweetRepository tweetRepo;
    private final PantipPostRepository pantipRepo;
    private final TyphoonAnalysisRepository typhoonRepo;
    private final ObjectMapper mapper = new ObjectMapper();

    public TyphoonBackfillService(
            LlmAnalyzer analyzer,
            TweetRepository tweetRepo,
            PantipPostRepository pantipRepo,
            TyphoonAnalysisRepository typhoonRepo
    ) {
        this.analyzer = analyzer;
        this.tweetRepo = tweetRepo;
        this.pantipRepo = pantipRepo;
        this.typhoonRepo = typhoonRepo;
    }

    @Transactional
    public int backfillTweets() {
        int inserted = 0;
        for (Tweet t : tweetRepo.findUnanalyzedTweets()) {
            String text = nvl(t.getText());
            if (text.isBlank()) {
                continue;
            }

            AnalyzeRequest req = new AnalyzeRequest();
            req.setText(text);
            req.setApp("web");
            req.setSource("twitter");

            AnalyzeResponse res = analyzer.analyze(req);
            String summary = extractSummaryOrFallback(res.getAnswerRaw(), text);

            TyphoonAnalysis row = new TyphoonAnalysis();
            row.setApp("twitter");
            row.setSourceTable("tweet");
            row.setSourceId(String.valueOf(t.getId()));
            row.setPostType("post");
            row.setLanguage("th");
            row.setSentiment(nullIfEmpty(res.getSentiment()));
            row.setTopic(nullIfEmpty(res.getTopic()));
            row.setSummary(summary);

            // ⭐ คะแนน & เหตุผล สำหรับ backfill
            if (res.getSentimentScore() != null) {
                row.setSentimentScore(
                        BigDecimal.valueOf(res.getSentimentScore())
                );
            }
            row.setRationaleSentiment(nullIfEmpty(res.getRationaleSentiment()));
            row.setRationaleIntent(nullIfEmpty(res.getRationaleIntent()));

            // created_at จากต้นทาง (แปลงเป็น LocalDateTime ถ้าเป็น String)
            row.setCreatedAt(parseTimeOrNull(String.valueOf(t.getCreatedAt())));
            row.setAnalyzedAtValue(LocalDateTime.now());

            if (!typhoonRepo.existsBySourceTableAndSourceId("tweet", String.valueOf(t.getId()))) {
                typhoonRepo.save(row);
                inserted++;
            }
        }
        return inserted;
    }

    @Transactional
    public int backfillPantip() {
        int inserted = 0;
        for (PantipPost p : pantipRepo.findUnanalyzedPosts()) {
            String base = nvl(p.getContent());
            if (base.isEmpty()) base = nvl(p.getPreview());
            if (base.isEmpty()) base = nvl(p.getTitle());
            if (base.isBlank()) {
                continue;
            }

            AnalyzeRequest req = new AnalyzeRequest();
            req.setText(base);
            req.setApp("web");
            req.setSource("pantip");

            AnalyzeResponse res = analyzer.analyze(req);
            String summary = extractSummaryOrFallback(res.getAnswerRaw(), base);

            TyphoonAnalysis row = new TyphoonAnalysis();
            row.setApp("pantip");
            row.setSourceTable("pantip_post");
            row.setSourceId(String.valueOf(p.getId()));
            row.setPostType("post");
            row.setLanguage("th");
            row.setSentiment(nullIfEmpty(res.getSentiment()));
            row.setTopic(nullIfEmpty(res.getTopic()));
            row.setSummary(summary);

            // ⭐ คะแนน & เหตุผล สำหรับ backfill
            if (res.getSentimentScore() != null) {
                row.setSentimentScore(
                        BigDecimal.valueOf(res.getSentimentScore())
                );
            }
            row.setRationaleSentiment(nullIfEmpty(res.getRationaleSentiment()));
            row.setRationaleIntent(nullIfEmpty(res.getRationaleIntent()));

            row.setCreatedAt(LocalDateTime.now());
            row.setAnalyzedAtValue(LocalDateTime.now());

            if (!typhoonRepo.existsBySourceTableAndSourceId("pantip_post", String.valueOf(p.getId()))) {
                typhoonRepo.save(row);
                inserted++;
            }
        }
        return inserted;
    }

    // ---------- utils ----------

    private String extractSummaryOrFallback(String answerRaw, String fallback) {
        if (answerRaw == null) return cut(fallback);
        try {
            JsonNode n = mapper.readTree(answerRaw);
            String s = n.path("summary").asText("");
            return s.isBlank() ? cut(fallback) : s;
        } catch (Exception ignore) {
            return cut(fallback);
        }
    }

    private String cut(String s) {
        String val = nvl(s);
        return val.length() > 200 ? val.substring(0, 200) : val;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }

    private LocalDateTime parseTimeOrNull(String createdAtStr) {
        try {
            return LocalDateTime.parse(createdAtStr.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }
}
