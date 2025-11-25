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
    // 0) SUMMARY (/api/analysis/summary)
    // ============================================================
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        List<Analysis> rows = repo.findAll();

        long pos = 0, neu = 0, neg = 0;

        for (Analysis a : rows) {
            String s = a.getFinalLabel();
            if (s == null || s.isBlank()) s = a.getSentiment();
            if (s == null) continue;

            switch (s.toLowerCase()) {
                case "positive" -> pos++;
                case "neutral" -> neu++;
                case "negative" -> neg++;
            }
        }

        // ===== totals =====
        Map<String, Object> totals = new HashMap<>();
        totals.put("mentions", rows.size());
        totals.put("positive", pos);
        totals.put("neutral", neu);
        totals.put("negative", neg);

        // ===== donut chart data =====
        List<Map<String, Object>> sentimentList = List.of(
                Map.of("label", "Positive", "value", pos),
                Map.of("label", "Neutral", "value", neu),
                Map.of("label", "Negative", "value", neg)
        );

        // ===== Trend (count by createdAt) =====
        Map<String, Long> byDate = new LinkedHashMap<>();
        for (Analysis a : rows) {
            String created = a.getCreatedAt();
            if (created == null || created.isBlank()) continue;
            if (created.length() >= 10) created = created.substring(0, 10);

            byDate.put(created, byDate.getOrDefault(created, 0L) + 1);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (var e : byDate.entrySet()) {
            trend.add(Map.of("date", e.getKey(), "count", e.getValue()));
        }

        // ===== Top Faculties (2 columns: name, count) =====
        List<Object[]> facRows = repo.getFacultySummary();
        List<Map<String, Object>> topFaculties = new ArrayList<>();

        for (Object[] r : facRows) {
            String faculty = (String) r[0];
            long totalCount = ((Number) r[1]).longValue();

            topFaculties.add(Map.of(
                    "name", faculty,
                    "count", totalCount
            ));
        }

        Map<String, Object> res = new HashMap<>();
        res.put("totals", totals);
        res.put("sentiment", sentimentList);
        res.put("trend", trend);
        res.put("topFaculties", topFaculties);

        return res;
    }

    // ============================================================
    // 1) วิเคราะห์ข้อความเดียว
    // ============================================================
    @PostMapping("/text")
    public Map<String, Object> analyzeSingle(@RequestBody Map<String, String> body) {
        String text = body.get("text");

        // วิเคราะห์ด้วย ONNX
        OnnxSentimentService.SentimentResult quick = onnx.analyze(text);

        // ปรับด้วย custom keyword
        String finalLabel = customKeywordService.applyCustomSentiment(text, quick.getLabel());

        // faculty อาจเป็น null ได้ → ถ้า null ให้ใช้ "ไม่ระบุ"
        String faculty = null;
        try {
            // เผื่อคุณยังไม่ได้ใส่ getFaculty ใน SentimentResult
            faculty = (String) quick.getClass().getMethod("getFaculty").invoke(quick);
        } catch (Exception ignore) {
            // ถ้าไม่มี method ก็ปล่อยไว้เป็น null
        }
        if (faculty == null || faculty.isBlank()) {
            faculty = "ไม่ระบุ";
        }

        Map<String, Object> res = new HashMap<>();
        res.put("text", text);
        res.put("sentimentLabel", finalLabel);     // label หลังปรับแล้ว
        res.put("modelLabel", quick.getLabel());   // label จากโมเดลดิบ
        res.put("sentimentScore", quick.getScore());
        res.put("faculty", faculty);               // ชื่อคณะ (หรือ "ไม่ระบุ")
        res.put("absaRaw", null);

        return res;
    }


    // ============================================================
    // 2) ดึง analysis ทั้งหมด
    // ============================================================
    @GetMapping
    public List<Map<String, Object>> getAnalysis() {
        List<Analysis> rows = repo.findAll();

        return rows.stream().map(r -> {
            String original = r.getSentiment();
            String finalLabel = customKeywordService.applyCustomSentiment(
                    r.getText(), original
            );

            return Map.<String, Object>ofEntries(
                    Map.entry("id", r.getId()),
                    Map.entry("tweetId", r.getId()),
                    Map.entry("text", r.getText()),
                    Map.entry("sentimentLabel", finalLabel),
                    Map.entry("modelLabel", original),
                    Map.entry("faculty", r.getFaculty()),
                    Map.entry("analyzedAt", r.getCreatedAt()),
                    Map.entry("createdAt", r.getCreatedAt()),
                    Map.entry("topics", List.of(r.getText())),
                    Map.entry("source", r.getPlatform()),
                    Map.entry("finalLabel", finalLabel)
            );
        }).toList();
    }

    // ============================================================
    // 3) Tweet dates
    // ============================================================
    @GetMapping("/tweet-dates")
    public List<String> getTweetDates() {
        return repo.findAll().stream()
                .map(Analysis::getCreatedAt)
                .toList();
    }

    // ============================================================
    // 4) ผู้ใช้แก้ sentiment ด้วยตัวเอง
    // ============================================================
    @PutMapping("/sentiment/update/{id}")
    public Map<String, Object> updateSentiment(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String newSentiment = body.get("sentiment");

        return repo.findById(id)
                .map(a -> {
                    a.setSentiment(newSentiment);
                    a.setFinalLabel(newSentiment);
                    repo.save(a);

                    // ✅ ตรงนี้ใช้ HashMap แทน Map.of → ได้ Map<String,Object>
                    Map<String, Object> res = new HashMap<>();
                    res.put("status", "success");
                    res.put("id", id);
                    res.put("newSentiment", newSentiment);
                    return res;
                })
                .orElseGet(() -> {
                    Map<String, Object> res = new HashMap<>();
                    res.put("status", "error");
                    res.put("message", "ID not found");
                    res.put("id", id);
                    return res;
                });
    }

    // ============================================================
    // 5) รายชื่อคณะที่มีอยู่
    // ============================================================
    @GetMapping("/faculty-list")
    public Set<String> getAllFaculty() {
        Set<String> fac = new HashSet<>();
        for (Analysis r : repo.findAll()) {
            if (r.getFaculty() != null) fac.add(r.getFaculty().trim());
        }
        return fac;
    }

    // ============================================================
    // 6) Batch rebuild ทั้งหมด
    // ============================================================
    @PostMapping("/batch/rebuild")
    @Transactional
    public Map<String, Object> rebuildAnalysis() {
        int t = analyzeTweets();
        int p = analyzePantipPosts();
        int c = analyzePantipComments();

        return Map.of(
                "status", "ok",
                "tweets_inserted", t,
                "pantip_posts_inserted", p,
                "pantip_comments_inserted", c,
                "total", t + p + c
        );
    }

    // ============================================================
    // 7) Batch — Pantip Only
    // ============================================================
    @PostMapping("/batch/pantip")
    @Transactional
    public Map<String, Object> rebuildPantipOnly() {
        int p = analyzePantipPosts();
        int c = analyzePantipComments();
        return Map.of(
                "status", "ok",
                "pantip_posts_inserted", p,
                "pantip_comments_inserted", c,
                "total", p + c
        );
    }

    // ================================ Helper ================================

    private int analyzeTweets() {
        List<Tweet> list = tweetRepo.findAll();
        int inserted = 0;

        for (Tweet t : list) {
            String id = "tw-" + t.getId();
            if (repo.existsById(id)) continue;
            if (t.getText() == null || t.getText().isBlank()) continue;

            var quick = onnx.analyze(t.getText());
            String faculty = quick.getFaculty();
            if (faculty == null || faculty.isBlank()) faculty = "ไม่ระบุ";

            String finalLabel =
                    customKeywordService.applyCustomSentiment(
                            t.getText(), quick.getLabel()
                    );

            Analysis a = new Analysis();
            a.setId(id);
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

    private int analyzePantipPosts() {
        List<PantipPost> list = pantipPostRepo.findAll();
        int inserted = 0;

        for (PantipPost p : list) {
            String id = "pt-" + p.getId();
            if (repo.existsById(id)) continue;
            if (p.getContent() == null || p.getContent().isBlank()) continue;

            var quick = onnx.analyze(p.getContent());
            String faculty = quick.getFaculty();
            if (faculty == null || faculty.isBlank()) faculty = "ไม่ระบุ";

            String finalLabel =
                    customKeywordService.applyCustomSentiment(
                            p.getContent(), quick.getLabel()
                    );

            Analysis a = new Analysis();
            a.setId(id);
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

    private int analyzePantipComments() {
        List<PantipComment> list = pantipCommentRepo.findAll();
        int inserted = 0;

        for (PantipComment c : list) {
            String id = "cmt-" + c.getId();
            if (repo.existsById(id)) continue;
            if (c.getText() == null || c.getText().isBlank()) continue;

            var quick = onnx.analyze(c.getText());
            String faculty = quick.getFaculty();
            if (faculty == null || faculty.isBlank()) faculty = "ไม่ระบุ";

            String finalLabel =
                    customKeywordService.applyCustomSentiment(
                            c.getText(), quick.getLabel()
                    );

            Analysis a = new Analysis();
            a.setId(id);
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
