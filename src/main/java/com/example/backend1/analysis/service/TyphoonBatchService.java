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
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashSet;
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

    // --------------------------------------------------
    // 1) วิเคราะห์ Twitter
    // --------------------------------------------------
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
            req.setTemperature(0.3);
            req.setMaxTokens(256);
            req.setSave(false);

            AnalyzeResponse res;
            try {
                res = llmAnalyzer.analyze(req);   // ⬅️ ตรงนี้ LlmAnalyzer จัด topic + sentiment ให้ครบแล้ว
            } catch (Exception e) {
                System.err.println("❌ Error analyzing tweet ID " + t.getId() + ": " + e.getMessage());
                continue;
            }

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

            // sentiment / topic หลังจากผ่าน rule LlmAnalyzer แล้ว
            row.setSentiment(nullIfEmpty(res.getSentiment()));
            row.setTopic(nullIfEmpty(res.getTopic()));
            row.setSummary(extractSummaryOrFallback(res.getAnswerRaw(), text));

            // คะแนน & เหตุผล
            if (res.getSentimentScore() != null) {
                row.setSentimentScore(
                        java.math.BigDecimal.valueOf(res.getSentimentScore())
                );
            }
            row.setRationaleSentiment(nullIfEmpty(res.getRationaleSentiment()));
            row.setRationaleIntent(nullIfEmpty(res.getRationaleIntent()));

            // ฟิลด์วิเคราะห์เชิงลึก (เก็บเป็น String ตาม entity)
            row.setToxicity(nullIfEmpty(res.getToxicity()));
            row.setNsfw(nullIfEmpty(res.getNsfw()));
            row.setFacultyCode(extractFacultyCode(res.getFacultyGuessJson()));
            row.setTopicsJson(buildTopicsJson(res));

            // created_at จากต้นทางถ้า parse ได้
            LocalDateTime createdAt = parseTimeOrNull(safeTweetCreatedAt(t));
            row.setCreatedAt(createdAt != null ? createdAt : LocalDateTime.now());

            // analyzed_at ใช้ helper non-bean
            row.setAnalyzedAtValue(LocalDateTime.now());

            typhoonRepo.save(row);

            // update ต้นทางว่า analyzed แล้ว (ใช้ sentiment string ตรง ๆ)
            t.setSentiment(res.getSentiment());
            tweetRepo.save(t);

            inserted++;
        }
        return inserted;
    }

    // --------------------------------------------------
    // 2) วิเคราะห์ Pantip
    // --------------------------------------------------
    @Transactional
    public int analyzePantip() {
        int inserted = 0;
        List<PantipPost> posts = pantipRepo.findUnanalyzedPosts();

        for (PantipPost p : posts) {
            String base = firstNonBlank(
                    nvl(p.getContent()),
                    nvl(p.getPreview()),
                    nvl(p.getTitle())
            );
            if (base.isBlank()) continue;

            AnalyzeRequest req = new AnalyzeRequest();
            req.setText(base);
            req.setApp("web");
            req.setSource("pantip");
            req.setModel("qwen2.5:7b-instruct");
            req.setTemperature(0.3);
            req.setMaxTokens(256);
            req.setSave(false);

            AnalyzeResponse res;
            try {
                res = llmAnalyzer.analyze(req);   // ⬅️ ตรงนี้เหมือนกัน LlmAnalyzer จัด topic + sentiment ให้แล้ว
            } catch (Exception e) {
                System.err.printf("❌ Error analyzing pantip post ID %s: %s%n",
                        p.getId(), e.getMessage());
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

            // คะแนน & เหตุผล
            if (res.getSentimentScore() != null) {
                row.setSentimentScore(
                        java.math.BigDecimal.valueOf(res.getSentimentScore())
                );
            }
            row.setRationaleSentiment(nullIfEmpty(res.getRationaleSentiment()));
            row.setRationaleIntent(nullIfEmpty(res.getRationaleIntent()));

            // ฟิลด์วิเคราะห์เชิงลึก
            row.setToxicity(nullIfEmpty(res.getToxicity()));
            row.setNsfw(nullIfEmpty(res.getNsfw()));
            row.setFacultyCode(extractFacultyCode(res.getFacultyGuessJson()));
            row.setTopicsJson(buildTopicsJson(res));

            row.setCreatedAt(LocalDateTime.now());
            row.setAnalyzedAtValue(LocalDateTime.now());

            typhoonRepo.save(row);

            p.setSentiment(res.getSentiment());
            pantipRepo.save(p);

            inserted++;
        }
        return inserted;
    }

    // --------------------------------------------------
    // utils
    // --------------------------------------------------

    /**
     * สรุป + เหตุผล + หลักฐาน
     */
    private String extractSummaryOrFallback(String answerRaw, String fallback) {
        if (answerRaw == null || answerRaw.isBlank()) {
            return cut(fallback);
        }

        try {
            JsonNode n = mapper.readTree(answerRaw);

            String summary = n.path("summary").asText("").trim();
            String reason  = n.path("reason").asText("").trim();

            List<String> evidences = new ArrayList<>();
            if (n.has("evidence") && n.get("evidence").isArray()) {
                for (JsonNode e : n.get("evidence")) {
                    String text = e.asText("").trim();
                    if (!text.isEmpty()) {
                        evidences.add(text);
                    }
                }
            }

            String base = summary.isBlank() ? cut(fallback) : summary;
            StringBuilder sb = new StringBuilder(base);

            if (!reason.isBlank()) {
                sb.append(" | เหตุผล: ").append(reason);
            }

            if (!evidences.isEmpty()) {
                LinkedHashSet<String> uniqueEvidence = new LinkedHashSet<>(evidences);
                sb.append(" | หลักฐาน: ").append(String.join(", ", uniqueEvidence));
            }

            return cut(sb.toString());
        } catch (Exception ex) {
            return cut(fallback);
        }
    }

    /**
     * ดึง faculty_code จาก JSON faculty_guess
     */
    private String extractFacultyCode(String facultyGuessJson) {
        if (facultyGuessJson == null || facultyGuessJson.isBlank()) return null;
        try {
            JsonNode n = mapper.readTree(facultyGuessJson);

            String code = n.path("faculty_code").asText("").trim();
            if (code.isEmpty()) {
                code = n.path("code").asText("").trim(); // เผื่อ JSON เก่า
            }

            if (code.isEmpty() || "unknown".equalsIgnoreCase(code)) {
                return null;
            }
            return code;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * สร้าง JSON สำหรับเก็บใน topics_json
     */
    private String buildTopicsJson(AnalyzeResponse res) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("topic", nvl(res.getTopic()));
            root.put("intent", nvl(res.getIntent()));
            root.put("utcc_relevance", nvl(res.getUtccRelevance()));
            root.put("actor", nvl(res.getActor()));
            root.put("hidden_meaning", nvl(res.getHiddenMeaning()));
            root.put("emotion", nvl(res.getEmotion()));
            root.put("impact_level", nvl(res.getImpactLevel()));
            return root.toString();
        } catch (Exception e) {
            return null;
        }
    }

    private String safeTweetCreatedAt(Tweet t) {
        try {
            return String.valueOf(t.getCreatedAt());
        } catch (Exception e) {
            return null;
        }
    }

    private LocalDateTime parseTimeOrNull(String createdAtStr) {
        if (createdAtStr == null || createdAtStr.isBlank()) return null;
        try {
            return LocalDateTime.parse(createdAtStr.replace(' ', 'T'));
        } catch (Exception e) {
            return null;
        }
    }

    private String firstNonBlank(String... vals) {
        for (String v : vals) {
            if (v != null && !v.isBlank()) return v;
        }
        return "";
    }

    private String cut(String s) {
        String v = nvl(s);
        return v.length() > 200 ? v.substring(0, 200) : v;
    }

    private String nvl(String s) {
        return s == null ? "" : s;
    }

    private String nullIfEmpty(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}
