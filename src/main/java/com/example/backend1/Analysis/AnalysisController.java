package com.example.backend1.Analysis;

import com.example.backend1.CustomKeywords.CustomKeywordService;
import com.example.backend1.Faculty.Faculty;
import com.example.backend1.Pantip.PantipComment;
import com.example.backend1.Pantip.PantipCommentRepository;
import com.example.backend1.Pantip.PantipPost;
import com.example.backend1.Pantip.PantipPostRepository;
import com.example.backend1.Twitter.Tweet;
import com.example.backend1.Twitter.TweetRepository;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.*;

@RestController
@RequestMapping("/analysis")   // /analysis ตรงกับ frontend
public class AnalysisController {

    private final OnnxSentimentService onnx;
    private final AnalysisRepository repo;
    private final TweetRepository tweetRepo;
    private final PantipPostRepository pantipPostRepo;
    private final PantipCommentRepository pantipCommentRepo;
    private final CustomKeywordService customKeywordService;

    // ตารางกลาง analysis_custom_keyword
    private final AnalysisCustomKeywordRepo ackRepo;


    public AnalysisController(
            OnnxSentimentService onnx,
            AnalysisRepository repo,
            TweetRepository tweetRepo,
            PantipPostRepository pantipPostRepo,
            PantipCommentRepository pantipCommentRepo,
            CustomKeywordService customKeywordService,
            AnalysisCustomKeywordRepo ackRepo
    ) {
        this.onnx = onnx;
        this.repo = repo;
        this.tweetRepo = tweetRepo;
        this.pantipPostRepo = pantipPostRepo;
        this.pantipCommentRepo = pantipCommentRepo;
        this.customKeywordService = customKeywordService;
        this.ackRepo = ackRepo;
    }

    // helper: เซฟความสัมพันธ์ analysis ↔ custom_keywords
    private void saveCustomKeywordLinks(Analysis a, String text) {
        if (text == null || text.isBlank()) return;

        List<Long> matchedIds = customKeywordService.getMatchedKeywordIds(text);
        for (Long kid : matchedIds) {
            ackRepo.save(new AnalysisCustomKeyword(a.getId(), kid));
        }
    }

    // 1.4) Playground: ให้ผู้ใช้ลองพิมพ์ข้อความ แล้วให้ ONNX วิเคราะห์สด
    //      POST /analysis/eval/try
    //      body JSON: { "text": "..." }
    @PostMapping("/eval/try")
    public Map<String, Object> tryEvaluateText(@RequestBody Map<String, String> body) {

        String text = Optional.ofNullable(body.get("text"))
                .orElse("")
                .trim();

        if (text.isEmpty()) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "ต้องระบุ text"
            );
        }

        try {
            OnnxSentimentService.SentimentResult res = onnx.analyze(text);

            String label = Optional.ofNullable(res.getLabel())
                    .orElse("neutral");

            Double score = null;
            try {
                score = res.getScore();
            } catch (Exception ignore) {
                // ถ้าไม่มีคะแนนในโมเดลก็ปล่อยให้เป็น null
            }

            Map<String, Object> resp = new HashMap<>();
            resp.put("text", text);
            resp.put("label", label);
            resp.put("sentimentScore", score);

            return resp;

        } catch (Exception ex) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR,
                    "วิเคราะห์ข้อความไม่สำเร็จ: " + ex.getMessage()
            );
        }
    }

    // 2) SUMMARY  (ไม่ใช้ query พิเศษ, คำนวณเองทั้งหมด)
    @GetMapping("/summary")
    public Map<String, Object> getSummary() {

        List<Analysis> rows = repo.findAll();
        long pos = 0, neu = 0, neg = 0;

        // นับ sentiment
        for (Analysis a : rows) {
            String s = (a.getFinalLabel() == null || a.getFinalLabel().isBlank())
                    ? a.getSentiment()
                    : a.getFinalLabel();

            if (s == null) continue;

            switch (s.toLowerCase()) {
                case "positive" -> pos++;
                case "neutral"  -> neu++;
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

        // trend รายวัน
        Map<String, Long> byDate = new TreeMap<>();
        for (Analysis a : rows) {
            String created = a.getCreatedAt();
            if (created == null || created.isBlank()) continue;

            created = created.substring(0, Math.min(10, created.length())); // แค่ yyyy-MM-dd
            byDate.put(created, byDate.getOrDefault(created, 0L) + 1);
        }

        List<Map<String, Object>> trend = new ArrayList<>();
        for (var e : byDate.entrySet()) {
            trend.add(Map.of("date", e.getKey(), "count", e.getValue()));
        }

        // topFaculties – นับเองจาก a.getFaculty()
        Map<String, Long> facCounter = new HashMap<>();
        for (Analysis a : rows) {
            String facName = (a.getFaculty() == null || a.getFaculty().isBlank())
                    ? "ไม่ระบุ"
                    : a.getFaculty();
            facCounter.put(facName, facCounter.getOrDefault(facName, 0L) + 1);
        }

        // แปลงเป็น list แล้ว sort จากมากไปน้อย
        List<Map<String, Object>> topFaculties = new ArrayList<>();
        facCounter.entrySet().stream()
                .sorted((e1, e2) -> Long.compare(e2.getValue(), e1.getValue()))
                .forEach(e -> {
                    Map<String, Object> m = new HashMap<>();
                    m.put("name", e.getKey());
                    m.put("count", e.getValue());
                    topFaculties.add(m);
                });

        return Map.of(
                "totals", totals,
                "sentiment", sentimentList,
                "trend", trend,
                "topFaculties", topFaculties
        );
    }

    // 3) วิเคราะห์ข้อความเดียว
    @PostMapping("/text")
    public Map<String, Object> analyzeSingle(@RequestBody Map<String, String> body) {
        String text = body.getOrDefault("text", "");

        var quick = onnx.analyze(text);
        String baseLabel = Optional.ofNullable(quick.getLabel()).orElse("neutral");

        String finalLabel;
        try {
            finalLabel = customKeywordService.applyCustomSentiment(text, baseLabel);
        } catch (Exception ex) {
            // กัน error ไม่ให้ 500
            finalLabel = baseLabel;
        }

        String faculty = Optional.ofNullable(quick.getFaculty()).orElse("ไม่ระบุ");

        Map<String, Object> resp = new HashMap<>();
        resp.put("text", text);
        resp.put("sentimentLabel", finalLabel);
        resp.put("modelLabel", baseLabel);
        resp.put("sentimentScore", quick.getScore());
        resp.put("faculty", faculty);
        resp.put("absaRaw", null);

        return resp;
    }

    // 4) ดึงทั้งหมด (ใช้บนหน้า Dashboard / Mentions / Trends)
    @GetMapping
    public List<Map<String, Object>> getAnalysis() {
        List<Analysis> rows = repo.findAll();
        List<Map<String, Object>> result = new ArrayList<>();

        for (Analysis r : rows) {
            String baseLabel = Optional.ofNullable(r.getSentiment()).orElse("neutral");

            String finalLabel;
            try {
                finalLabel = customKeywordService.applyCustomSentiment(
                        r.getText(),
                        baseLabel
                );
            } catch (Exception ex) {
                finalLabel = baseLabel;
            }

            String originalUrl = null;

            if ("twitter".equals(r.getPlatform()) && r.getTweet() != null) {
                originalUrl = "https://x.com/i/web/status/" + r.getTweet().getId();
            }
            else if ("pantip_post".equals(r.getPlatform()) && r.getPantipPost() != null) {
                originalUrl = r.getPantipPost().getUrl();
            }
            else if ("pantip_comment".equals(r.getPlatform()) && r.getPantipComment() != null) {
                if (r.getPantipComment().getPost() != null) {
                    originalUrl = r.getPantipComment().getPost().getUrl();
                }
            }

            Map<String, Object> m = new HashMap<>();
            m.put("id", r.getId());
            m.put("text", r.getText());
            m.put("sentimentLabel", finalLabel);
            m.put("modelLabel", r.getSentiment());
            m.put("faculty", r.getFaculty());
            m.put("analyzedAt", r.getCreatedAt());
            m.put("createdAt", r.getCreatedAt());
            m.put("source", r.getPlatform());
            m.put("finalLabel", finalLabel);

            // ✅ ส่งลิงก์ต้นฉบับออกไป
            m.put("originalUrl", originalUrl);

            result.add(m);
        }

        return result;
    }

    // 5) Tweet dates
    @GetMapping("/tweet-dates")
    public List<String> getTweetDates() {
        return repo.findAll().stream()
                .map(Analysis::getCreatedAt)
                .filter(Objects::nonNull)
                .toList();
    }

    // 6) ผู้ใช้แก้ sentiment
    @PutMapping("/sentiment/update/{id}")
    public Map<String, String> updateSentiment(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String newSentiment = body.getOrDefault("sentiment", "neutral");

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

    // 7) Batch rebuild ALL
    @PostMapping("/batch/rebuild")
    @Transactional
    public Map<String, Object> rebuildAnalysis() {
        int t = analyzeTweets();
        int p = analyzePantipPosts();
        int c = analyzePantipComments();

        return Map.of("status", "ok", "tweets", t, "posts", p, "comments", c);
    }

    // batch สำหรับ Pantip อย่างเดียว
    @PostMapping("/batch/pantip")
    @Transactional
    public Map<String, Object> rebuildPantipOnly() {
        int p = analyzePantipPosts();
        int c = analyzePantipComments();

        return Map.of(
                "status", "ok",
                "posts", p,
                "comments", c,
                "total", p + c
        );
    }

    // 8) Helper — Tweet
    private int analyzeTweets() {
        int inserted = 0;

        for (Tweet t : tweetRepo.findAll()) {
            String id = "tw-" + t.getId();

            if (repo.existsById(id) || t.getText() == null || t.getText().isBlank()) continue;

            var quick = onnx.analyze(t.getText());
            String baseLabel = Optional.ofNullable(quick.getLabel()).orElse("neutral");

            String finalLabel;
            try {
                finalLabel = customKeywordService.applyCustomSentiment(
                        t.getText(),
                        baseLabel
                );
            } catch (Exception ex) {
                finalLabel = baseLabel;
            }

            String facName = Optional.ofNullable(quick.getFacultyName()).orElse("ไม่ระบุ");

            Analysis a = new Analysis();
            a.setId(id);
            a.setTweet(t);
            a.setText(t.getText());
            a.setCreatedAt(t.getCreatedAt());
            a.setPlatform("twitter");
            a.setFaculty(facName);
            a.setSentimentScore(quick.getScore());

            if (quick.getFacultyId() != null) {
                Faculty fac = new Faculty();
                fac.setId(quick.getFacultyId());
                a.setFacultyRef(fac);
            } else {
                a.setFacultyRef(null);
            }

            a.setSentiment(finalLabel);
            a.setFinalLabel(finalLabel);

            repo.save(a);
            inserted++;

            saveCustomKeywordLinks(a, t.getText());
        }
        return inserted;
    }

    // 9) Helper — Pantip Post
    private int analyzePantipPosts() {
        int inserted = 0;

        for (PantipPost p : pantipPostRepo.findAll()) {
            String id = "pt-" + p.getId();

            if (repo.existsById(id) || p.getContent() == null || p.getContent().isBlank()) continue;

            var quick = onnx.analyze(p.getContent());
            String baseLabel = Optional.ofNullable(quick.getLabel()).orElse("neutral");

            String finalLabel;
            try {
                finalLabel = customKeywordService.applyCustomSentiment(
                        p.getContent(),
                        baseLabel
                );
            } catch (Exception ex) {
                finalLabel = baseLabel;
            }

            String facName = Optional.ofNullable(quick.getFacultyName()).orElse("ไม่ระบุ");

            Analysis a = new Analysis();
            a.setId(id);
            a.setPantipPost(p);
            a.setText(p.getContent());
            a.setCreatedAt(p.getPostTime());
            a.setPlatform("pantip_post");
            a.setFaculty(facName);
            a.setSentimentScore(quick.getScore());

            if (quick.getFacultyId() != null) {
                Faculty fac = new Faculty();
                fac.setId(quick.getFacultyId());
                a.setFacultyRef(fac);
            } else {
                a.setFacultyRef(null);
            }

            a.setSentiment(finalLabel);
            a.setFinalLabel(finalLabel);

            repo.save(a);
            inserted++;

            saveCustomKeywordLinks(a, p.getContent());
        }
        return inserted;
    }

    // 10) Helper — Pantip Comment
    private int analyzePantipComments() {
        int inserted = 0;

        for (PantipComment c : pantipCommentRepo.findAll()) {
            String id = "cmt-" + c.getId();

            if (repo.existsById(id) || c.getText() == null || c.getText().isBlank()) continue;

            var quick = onnx.analyze(c.getText());
            String baseLabel = Optional.ofNullable(quick.getLabel()).orElse("neutral");

            String finalLabel;
            try {
                finalLabel = customKeywordService.applyCustomSentiment(
                        c.getText(),
                        baseLabel
                );
            } catch (Exception ex) {
                finalLabel = baseLabel;
            }

            String facName = Optional.ofNullable(quick.getFacultyName()).orElse("ไม่ระบุ");

            Analysis a = new Analysis();
            a.setId(id);
            a.setPantipComment(c);
            a.setText(c.getText());
            a.setCreatedAt(c.getCommentedAt());
            a.setPlatform("pantip_comment");
            a.setFaculty(facName);
            a.setSentimentScore(quick.getScore());

            if (quick.getFacultyId() != null) {
                Faculty fac = new Faculty();
                fac.setId(quick.getFacultyId());
                a.setFacultyRef(fac);
            } else {
                a.setFacultyRef(null);
            }

            a.setSentiment(finalLabel);
            a.setFinalLabel(finalLabel);

            repo.save(a);
            inserted++;

            saveCustomKeywordLinks(a, c.getText());
        }
        return inserted;
    }

    // 11) ผู้ใช้แก้ "คณะ" เอง
    @PutMapping("/faculty/update/{id}")
    public Map<String, Object> updateFaculty(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String newFaculty = body.getOrDefault("faculty", "ไม่ระบุ");

        return repo.findById(id)
                .map(a -> {
                    a.setFaculty(newFaculty);
                    repo.save(a);

                    Map<String, Object> res = new HashMap<>();
                    res.put("status", "success");
                    res.put("id", id);
                    res.put("newFaculty", newFaculty);
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

    // 12) ค้นหา / explain ตาม id
    @GetMapping("/{id}")
    public Map<String, Object> getAnalysisById(@PathVariable String id) {

        return repo.findById(id)
                .map(r -> {
                    String baseLabel = Optional.ofNullable(r.getSentiment()).orElse("neutral");
                    String finalLabel;
                    try {
                        finalLabel = customKeywordService.applyCustomSentiment(
                                r.getText(),
                                baseLabel
                        );
                    } catch (Exception ex) {
                        finalLabel = baseLabel;
                    }

                    Map<String, Object> res = new HashMap<>();
                    res.put("id", r.getId());
                    res.put("text", r.getText());
                    res.put("sentimentLabel", finalLabel);
                    res.put("modelLabel", r.getSentiment());
                    res.put("faculty", r.getFaculty());
                    res.put("analyzedAt", r.getCreatedAt());
                    res.put("source", r.getPlatform());
                    return res;
                })
                .orElseGet(() -> Map.of(
                        "status", "error",
                        "message", "ไม่พบ ID นี้ในฐานข้อมูล",
                        "id", id
                ));
    }

    @GetMapping("/{id}/explain")
    public Map<String, Object> explain(@PathVariable String id) {

        return repo.findById(id)
                .map(a -> {
                    String baseLabel = Optional.ofNullable(a.getSentiment()).orElse("neutral");
                    String finalLabel;
                    try {
                        finalLabel = customKeywordService.applyCustomSentiment(
                                a.getText(),
                                baseLabel
                        );
                    } catch (Exception ex) {
                        finalLabel = baseLabel;
                    }

                    var matched = customKeywordService.getMatchedKeywords(a.getText());

                    List<Map<String, Object>> matchedKeywords = new ArrayList<>();
                    for (var k : matched) {
                        Map<String, Object> mk = new HashMap<>();
                        mk.put("id", k.getId());
                        mk.put("keyword", k.getKeyword());
                        mk.put("sentiment", k.getSentiment());
                        matchedKeywords.add(mk);
                    }

                    Map<String, Object> res = new HashMap<>();
                    res.put("id", a.getId());
                    res.put("text", a.getText());
                    res.put("modelLabel", a.getSentiment());
                    res.put("finalLabel", finalLabel);
                    res.put("sentimentScore", a.getSentimentScore());
                    res.put("faculty", a.getFaculty());
                    res.put("matchedKeywords", matchedKeywords);

                    return res;
                })
                .orElseGet(() -> Map.of(
                        "status", "error",
                        "message", "ไม่พบ ID นี้ในฐานข้อมูล",
                        "id", id
                ));
    }

    // 13) วิเคราะห์เฉพาะ Pantip ที่เพิ่งเพิ่มใหม่
    @PostMapping("/pantip/scan-new")
    @Transactional
    public Map<String, Object> analyzeNewPantip() {

        int newPosts    = analyzePantipPosts();
        int newComments = analyzePantipComments();

        return Map.of(
                "status", "ok",
                "newPosts", newPosts,
                "newComments", newComments
        );
    }
}
