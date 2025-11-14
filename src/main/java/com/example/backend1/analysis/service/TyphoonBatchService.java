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
import java.math.BigDecimal;

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
            // ลด temperature ให้ผลนิ่งขึ้น
            req.setTemperature(0.3);
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

            // เติมฟิลด์วิเคราะห์เชิงลึกเพิ่มเติม
            row.setToxicity(toxicityScore(res.getToxicity()));   // ← แปลงเป็น BigDecimal
            row.setNsfw(nullIfEmpty(res.getNsfw()));
            row.setFacultyCode(extractFacultyCode(res.getFacultyGuessJson()));
            row.setTopicsJson(buildTopicsJson(res));

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
            // ลด temperature ให้ผลนิ่งขึ้น
            req.setTemperature(0.3);
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

            // เติมฟิลด์วิเคราะห์เชิงลึกเพิ่มเติม
            row.setToxicity(toxicityScore(res.getToxicity()));   // ← แปลงเป็น BigDecimal
            row.setNsfw(nullIfEmpty(res.getNsfw()));
            row.setFacultyCode(extractFacultyCode(res.getFacultyGuessJson()));
            row.setTopicsJson(buildTopicsJson(res));

            row.setCreatedAt(LocalDateTime.now());
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
        // ถ้าไม่มีข้อมูลหรือเป็นค่าว่าง → ใช้ fallback (ข้อความดิบ)
        if (answerRaw == null || answerRaw.isBlank()) {
            return cut(fallback);
        }

        try {
            JsonNode n = mapper.readTree(answerRaw);

            // ดึง field ตามที่ prompt ใหม่กำหนด
            String summary = n.path("summary").asText("").trim();
            String reason  = n.path("reason").asText("").trim();

            // ดึง evidence เป็น list
            List<String> evidences = new ArrayList<>();
            if (n.has("evidence") && n.get("evidence").isArray()) {
                for (JsonNode e : n.get("evidence")) {
                    String text = e.asText("").trim();
                    if (!text.isEmpty()) {
                        evidences.add(text);
                    }
                }
            }

            // ---------- ประมวลผล summary ----------
            // ถ้า summary ว่าง → ใช้ fallback
            String base = summary.isBlank() ? cut(fallback) : summary;

            // สร้าง StringBuilder สำหรับข้อความสุดท้าย
            StringBuilder sb = new StringBuilder(base);

            // ---------- เพิ่มเหตุผล (reason) ----------
            if (!reason.isBlank()) {
                sb.append(" | เหตุผล: ").append(reason);
            }

            // ---------- เพิ่มหลักฐาน (evidence) ----------
            if (!evidences.isEmpty()) {
                // ลบคำที่ซ้ำกันและทำให้อ่านง่าย (กันโมเดลตอบซ้ำ)
                LinkedHashSet<String> uniqueEvidence = new LinkedHashSet<>(evidences);
                sb.append(" | หลักฐาน: ").append(String.join(", ", uniqueEvidence));
            }

            // ตัดความยาวให้ปลอดภัยก่อนเก็บ DB
            return cut(sb.toString());

        } catch (Exception ex) {
            // ถ้า JSON พัง หรือ parse ไม่ได้ → ใช้ fallback
            return cut(fallback);
        }
    }

    // ดึง faculty_code จาก JSON เช่น {"code":"BUA","name":"บริหารธุรกิจ","reason":"..."}
    private String extractFacultyCode(String facultyGuessJson) {
        if (facultyGuessJson == null || facultyGuessJson.isBlank()) return null;
        try {
            JsonNode n = mapper.readTree(facultyGuessJson);
            String code = n.path("code").asText("").trim();
            if (code.isEmpty() || "unknown".equalsIgnoreCase(code)) {
                return null;
            }
            return code;
        } catch (Exception e) {
            return null;
        }
    }

    // สร้าง JSON สำหรับเก็บใน topics_json
    private String buildTopicsJson(AnalyzeResponse res) {
        try {
            ObjectNode root = mapper.createObjectNode();
            root.put("topic", nvl(res.getTopic()));
            root.put("intent", nvl(res.getIntent()));
            root.put("utcc_relevance", nvl(res.getUtccRelevance()));
            root.put("actor", nvl(res.getActor()));
            root.put("hidden_meaning", nvl(res.getHiddenMeaning()));
            return root.toString();
        } catch (Exception e) {
            return null;
        }
    }

    // แปลงค่า toxicity จาก string → BigDecimal ให้ตรงกับ type ใน TyphoonAnalysis
    private BigDecimal toxicityScore(String toxicity) {
        String v = nvl(toxicity).toLowerCase();

        switch (v) {
            case "":
            case "none":
                return BigDecimal.ZERO;              // 0 = ไม่มีความเป็นพิษ
            case "low":
                return BigDecimal.ONE;               // 1 = ต่ำ
            case "medium":
                return BigDecimal.valueOf(2);        // 2 = ปานกลาง
            case "high":
                return BigDecimal.valueOf(3);        // 3 = สูง
            default:
                return null;                         // ถ้า LLM ตอบเพี้ยน ให้เป็น null
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
