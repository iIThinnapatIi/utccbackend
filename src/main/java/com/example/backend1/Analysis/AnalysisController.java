package com.example.backend1.Analysis;

import com.example.backend1.CustomKeywords.CustomKeywordService;
import com.example.backend1.Pantip.PantipComment;
import com.example.backend1.Pantip.PantipCommentRepository;
import com.example.backend1.Pantip.PantipPost;
import com.example.backend1.Pantip.PantipPostRepository;
import com.example.backend1.Twitter.Tweet;
import com.example.backend1.Twitter.TweetRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.*;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final OnnxSentimentService onnx;
    private final AnalysisRepository repo;
    private final TweetRepository tweetRepo;
    private final PantipPostRepository pantipPostRepo;
    private final PantipCommentRepository pantipCommentRepo;
    private final CustomKeywordService customKeywordService;

    public AnalysisController(
            OnnxSentimentService onnx,
            AnalysisRepository repo,
            TweetRepository tweetRepo,
            PantipPostRepository pantipPostRepo,
            PantipCommentRepository pantipCommentRepo,
            CustomKeywordService customKeywordService
    ) {
        this.onnx = onnx;
        this.repo = repo;
        this.tweetRepo = tweetRepo;
        this.pantipPostRepo = pantipPostRepo;
        this.pantipCommentRepo = pantipCommentRepo;
        this.customKeywordService = customKeywordService;
    }

    // ============================================================
    // SUMMARY
    // ============================================================
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        List<Analysis> rows = repo.findAll();
        long pos = 0, neu = 0, neg = 0;

        for (Analysis a : rows) {
            String s = (a.getFinalLabel() == null || a.getFinalLabel().isBlank())
                    ? a.getSentiment()
                    : a.getFinalLabel();

            if (s == null) continue;

            switch (s.toLowerCase()) {
                case "positive" -> pos++;
                case "neutral" -> neu++;
                case "negative" -> neg++;
            }
        }

        Map<String, Object> totals = new HashMap<>();
        totals.put("mentions", rows.size());
        totals.put("positive", pos);
        totals.put("neutral", neu);
        totals.put("negative", neg);

        List<Map<String, Object>> sentimentList = List.of(
                Map.of("label", "Positive", "value", pos),
                Map.of("label", "Neutral", "value", neu),
                Map.of("label", "Negative", "value", neg)
        );

        Map<String, Long> byDate = new LinkedHashMap<>();
        for (Analysis a : rows) {
            String created = a.getCreatedAt();
            if (created == null || created.isBlank()) continue;

            created = created.substring(0, Math.min(10, created.length()));
            byDate.put(created, byDate.getOrDefault(created, 0L) + 1);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (var e : byDate.entrySet()) {
            trend.add(Map.of("date", e.getKey(), "count", e.getValue()));
        }

        List<Object[]> facRows = repo.getFacultySummary();
        List<Map<String, Object>> topFaculties = new ArrayList<>();
        for (Object[] r : facRows) {
            topFaculties.add(Map.of("name", (String) r[0], "count", ((Number) r[1]).longValue()));
        }

        return Map.of(
                "totals", totals,
                "sentiment", sentimentList,
                "trend", trend,
                "topFaculties", topFaculties
        );
    }

    // ============================================================
    // วิเคราะห์ข้อความเดียว
    // ============================================================
    @PostMapping("/text")
    public Map<String, Object> analyzeSingle(@RequestBody Map<String, String> body) {
        String text = body.get("text");

        var quick = onnx.analyze(text);
        String finalLabel = customKeywordService.applyCustomSentiment(text, quick.getLabel());
        String faculty = quick.getFaculty() != null ? quick.getFaculty() : "ไม่ระบุ";

        return Map.of(
                "text", text,
                "sentimentLabel", finalLabel,
                "modelLabel", quick.getLabel(),
                "sentimentScore", quick.getScore(),
                "faculty", faculty,
                "absaRaw", null
        );
    }
    // ============================================================
    // ดึงทั้งหมด
    // ============================================================
    @GetMapping
    public List<Map<String, Object>> getAnalysis() {
        List<Analysis> rows = repo.findAll();

        return rows.stream().map(r -> {
            String finalLabel = customKeywordService.applyCustomSentiment(r.getText(), r.getSentiment());

            return Map.<String, Object>ofEntries(
                    Map.entry("id", r.getId()),
                    Map.entry("text", r.getText()),
                    Map.entry("sentimentLabel", finalLabel),
                    Map.entry("modelLabel", r.getSentiment()),
                    Map.entry("faculty", r.getFaculty()),
                    Map.entry("analyzedAt", r.getCreatedAt()),
                    Map.entry("createdAt", r.getCreatedAt()),
                    Map.entry("source", r.getPlatform()),
                    Map.entry("finalLabel", finalLabel)
            );
        }).toList();
    }

    // ============================================================
    // Tweet dates
    // ============================================================
    @GetMapping("/tweet-dates")
    public List<String> getTweetDates() {
        return repo.findAll().stream()
                .map(Analysis::getCreatedAt)
                .toList();
    }

    // ============================================================
    // ผู้ใช้แก้ sentiment
    // ============================================================
    @PutMapping("/sentiment/update/{id}")
    public Map<String, String> updateSentiment(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String newSentiment = body.get("sentiment");

        return repo.findById(id)
                .map(a -> {
                    a.setSentiment(newSentiment);
                    a.setFinalLabel(newSentiment);
                    repo.save(a);
                    return Map.of(
                            "status", "success",
                            "id", id,
                            "newSentiment", newSentiment
                    );
                })
                .orElseGet(() -> Map.of(
                        "status", "error",
                        "message", "ID not found",
                        "id", id
                ));
    }


    // ============================================================
    // Batch rebuild ALL
    // ============================================================
    @PostMapping("/batch/rebuild")
    @Transactional
    public Map<String, Object> rebuildAnalysis() {
        int t = analyzeTweets();
        int p = analyzePantipPosts();
        int c = analyzePantipComments();

        return Map.of("status", "ok", "tweets", t, "posts", p, "comments", c);
    }

    // ============================================================
    // Helper — Tweet
    // ============================================================
    private int analyzeTweets() {
        int inserted = 0;

        for (Tweet t : tweetRepo.findAll()) {
            String id = "tw-" + t.getId();
            if (repo.existsById(id) || t.getText() == null || t.getText().isBlank()) continue;

            var quick = onnx.analyze(t.getText());
            String finalLabel = customKeywordService.applyCustomSentiment(t.getText(), quick.getLabel());
            String faculty = quick.getFaculty() != null ? quick.getFaculty() : "ไม่ระบุ";

            Analysis a = new Analysis();
            a.setId(id);
            a.setTweet(t);                    // FK
            a.setText(t.getText());
            a.setCreatedAt(t.getCreatedAt());
            a.setPlatform("twitter");
            a.setFaculty(faculty);
            a.setSentiment(finalLabel);
            a.setFinalLabel(finalLabel);

            repo.save(a);
            inserted++;
        }
        return inserted;
    }

    // ============================================================
    // Helper — Pantip Post
    // ============================================================
    private int analyzePantipPosts() {
        int inserted = 0;

        for (PantipPost p : pantipPostRepo.findAll()) {
            String id = "pt-" + p.getId();
            if (repo.existsById(id) || p.getContent() == null || p.getContent().isBlank()) continue;

            var quick = onnx.analyze(p.getContent());
            String finalLabel = customKeywordService.applyCustomSentiment(p.getContent(), quick.getLabel());
            String faculty = quick.getFaculty() != null ? quick.getFaculty() : "ไม่ระบุ";

            Analysis a = new Analysis();
            a.setId(id);
            a.setPantipPost(p);               // FK
            a.setText(p.getContent());
            a.setCreatedAt(p.getPostTime());
            a.setPlatform("pantip_post");
            a.setFaculty(faculty);
            a.setSentiment(finalLabel);
            a.setFinalLabel(finalLabel);

            repo.save(a);
            inserted++;
        }
        return inserted;
    }

    // ============================================================
    // Helper — Pantip Comment
    // ============================================================
    private int analyzePantipComments() {
        int inserted = 0;

        for (PantipComment c : pantipCommentRepo.findAll()) {
            String id = "cmt-" + c.getId();
            if (repo.existsById(id) || c.getText() == null || c.getText().isBlank()) continue;

            var quick = onnx.analyze(c.getText());
            String finalLabel = customKeywordService.applyCustomSentiment(c.getText(), quick.getLabel());
            String faculty = quick.getFaculty() != null ? quick.getFaculty() : "ไม่ระบุ";

            Analysis a = new Analysis();
            a.setId(id);
            a.setPantipComment(c);            // FK
            a.setText(c.getText());
            a.setCreatedAt(c.getCommentedAt());
            a.setPlatform("pantip_comment");
            a.setFaculty(faculty);
            a.setSentiment(finalLabel);
            a.setFinalLabel(finalLabel);

            repo.save(a);
            inserted++;
        }
        return inserted;
    }
}