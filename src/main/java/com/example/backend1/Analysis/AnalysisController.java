package com.example.backend1.Analysis;

import com.example.backend1.CustomKeywords.CustomKeywordService;
import com.example.backend1.Pantip.PantipComment;
import com.example.backend1.Pantip.PantipCommentRepository;
import com.example.backend1.Pantip.PantipPost;
import com.example.backend1.Pantip.PantipPostRepository;
import com.example.backend1.Twitter.Tweet;
import com.example.backend1.Twitter.TweetRepository;
import java.util.List;
import java.util.HashMap;
import java.util.Map;


// ⭐ ใหม่: ใช้ Faculty เป็น FK
import com.example.backend1.Faculty.Faculty;

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

    // ตารางกลาง analysis_custom_keyword
    private final AnalysisCustomKeywordRepo ackRepo;

    // ⭐ ใหม่: ตาราง evaluation_samples
    private final EvaluationSampleRepo evalRepo;   // <--- ต้องมี ; ปิดท้าย

    public AnalysisController(
            OnnxSentimentService onnx,
            AnalysisRepository repo,
            TweetRepository tweetRepo,
            PantipPostRepository pantipPostRepo,
            PantipCommentRepository pantipCommentRepo,
            CustomKeywordService customKeywordService,
            AnalysisCustomKeywordRepo ackRepo,
            EvaluationSampleRepo evalRepo              // <--- เพิ่ม parameter ตรงนี้
    ) {
        this.onnx = onnx;
        this.repo = repo;
        this.tweetRepo = tweetRepo;
        this.pantipPostRepo = pantipPostRepo;
        this.pantipCommentRepo = pantipCommentRepo;
        this.customKeywordService = customKeywordService;
        this.ackRepo = ackRepo;
        this.evalRepo = evalRepo;                     // <--- ผูกค่าให้ field
    }


    // ✅ helper เล็ก ๆ เอาไว้เซฟความสัมพันธ์ analysis ↔ custom_keywords
    private void saveCustomKeywordLinks(Analysis a, String text) {
        if (text == null || text.isBlank()) return;

        // ใช้ service เดิมที่เขียนไว้แล้ว
        List<Long> matchedIds = customKeywordService.getMatchedKeywordIds(text);

        for (Long kid : matchedIds) {
            ackRepo.save(new AnalysisCustomKeyword(a.getId(), kid));
        }
    }

    // ============================================================
    // Model Evaluation: วัดความแม่นของโมเดลเทียบกับ label ที่คนให้
    // ============================================================
    @GetMapping("/eval")
    public Map<String, Object> evaluateModel() {

        List<EvaluationSample> samples = evalRepo.findAll();
        if (samples.isEmpty()) {
            return Map.of(
                    "status", "error",
                    "message", "ยังไม่มีข้อมูลในตาราง evaluation_samples"
            );
        }

        int total = samples.size();
        int correct = 0;

        Map<String, Integer> tp = new HashMap<>();
        Map<String, Integer> fp = new HashMap<>();
        Map<String, Integer> fn = new HashMap<>();

        for (EvaluationSample s : samples) {
            String gold = s.getTrueLabel().toLowerCase().trim();   // label ของคน
            OnnxSentimentService.SentimentResult res = onnx.analyze(s.getText());
            String pred = res.getLabel().toLowerCase().trim();     // label ของโมเดล

            if (gold.equals(pred)) {
                correct++;
                tp.put(gold, tp.getOrDefault(gold, 0) + 1);
            } else {
                fp.put(pred, fp.getOrDefault(pred, 0) + 1);
                fn.put(gold, fn.getOrDefault(gold, 0) + 1);
            }
        }

        double accuracy = (double) correct / total;

        Map<String, Map<String, Double>> perClass = new HashMap<>();
        for (String label : List.of("positive", "neutral", "negative")) {
            int tpL = tp.getOrDefault(label, 0);
            int fpL = fp.getOrDefault(label, 0);
            int fnL = fn.getOrDefault(label, 0);

            double precision = tpL + fpL == 0 ? 0.0 : (double) tpL / (tpL + fpL);
            double recall    = tpL + fnL == 0 ? 0.0 : (double) tpL / (tpL + fnL);
            double f1        = (precision + recall) == 0 ? 0.0 : 2 * precision * recall / (precision + recall);

            Map<String, Double> m = new HashMap<>();
            m.put("precision", precision);
            m.put("recall", recall);
            m.put("f1", f1);

            perClass.put(label, m);
        }

        Map<String, Object> res = new HashMap<>();
        res.put("status", "ok");
        res.put("totalSamples", total);
        res.put("accuracy", accuracy);
        res.put("perClass", perClass);

        return res;
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

        Map<String, Object> resp = new HashMap<>();
        resp.put("text", text);
        resp.put("sentimentLabel", finalLabel);
        resp.put("modelLabel", quick.getLabel());
        resp.put("sentimentScore", quick.getScore());
        resp.put("faculty", faculty);
        resp.put("absaRaw", null);

        return resp;
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

    // ⭐ ใหม่: batch สำหรับ Pantip อย่างเดียว (ให้ตรงกับ Keywords.jsx)
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

    // ============================================================
    // Helper — Tweet
    // ============================================================
    private int analyzeTweets() {
        int inserted = 0;

        for (Tweet t : tweetRepo.findAll()) {
            String id = "tw-" + t.getId();

            // ข้ามถ้ามีอยู่แล้ว หรือไม่มีข้อความ
            if (repo.existsById(id) || t.getText() == null || t.getText().isBlank()) continue;

            // 🔹 วิเคราะห์ด้วย ONNX
            var quick = onnx.analyze(t.getText());

            // 🔹 ปรับ sentiment ด้วย custom keyword
            String finalLabel = customKeywordService.applyCustomSentiment(
                    t.getText(),
                    quick.getLabel()
            );

            // 🔹 ชื่อคณะ (string) เอาไว้แสดงผล
            String facName = quick.getFacultyName() != null
                    ? quick.getFacultyName()
                    : "ไม่ระบุ";

            Analysis a = new Analysis();
            a.setId(id);
            a.setTweet(t);                    // FK
            a.setText(t.getText());
            a.setCreatedAt(t.getCreatedAt());
            a.setPlatform("twitter");

            // ⭐ เก็บทั้งชื่อคณะ (string) และ FK
            a.setFaculty(facName);
            a.setSentimentScore(quick.getScore());

            if (quick.getFacultyId() != null) {
                Faculty fac = new Faculty();
                fac.setId(quick.getFacultyId());    // แค่เซ็ต id พอ ให้ JPA จับ FK เอง
                a.setFacultyRef(fac);
            } else {
                a.setFacultyRef(null);
            }

            a.setSentiment(finalLabel);
            a.setFinalLabel(finalLabel);

            repo.save(a);
            inserted++;

            // ⭐ บันทึกความสัมพันธ์กับ custom_keywords
            saveCustomKeywordLinks(a, t.getText());
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

            // ข้ามถ้าวิเคราะห์แล้ว หรือไม่มี content
            if (repo.existsById(id) || p.getContent() == null || p.getContent().isBlank()) continue;

            // 🔹 วิเคราะห์ด้วย ONNX
            var quick = onnx.analyze(p.getContent());

            // 🔹 sentiment หลังใช้ custom keyword
            String finalLabel = customKeywordService.applyCustomSentiment(
                    p.getContent(),
                    quick.getLabel()
            );

            // 🔹 ชื่อคณะเอาไว้โชว์
            String facName = quick.getFacultyName() != null
                    ? quick.getFacultyName()
                    : "ไม่ระบุ";

            Analysis a = new Analysis();
            a.setId(id);
            a.setPantipPost(p);               // FK
            a.setText(p.getContent());
            a.setCreatedAt(p.getPostTime());
            a.setPlatform("pantip_post");

            // ⭐ เก็บทั้งชื่อคณะ และ FK
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

            // ⭐ บันทึกความสัมพันธ์กับ custom_keywords
            saveCustomKeywordLinks(a, p.getContent());
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

            // ข้ามถ้ามีแล้ว หรือไม่มีข้อความ
            if (repo.existsById(id) || c.getText() == null || c.getText().isBlank()) continue;

            // 🔹 วิเคราะห์ ONNX
            var quick = onnx.analyze(c.getText());

            // 🔹 ปรับ sentiment ด้วย custom keyword
            String finalLabel = customKeywordService.applyCustomSentiment(
                    c.getText(),
                    quick.getLabel()
            );

            // 🔹 ชื่อคณะ (โชว์)
            String facName = quick.getFacultyName() != null
                    ? quick.getFacultyName()
                    : "ไม่ระบุ";

            Analysis a = new Analysis();
            a.setId(id);
            a.setPantipComment(c);            // FK
            a.setText(c.getText());
            a.setCreatedAt(c.getCommentedAt());
            a.setPlatform("pantip_comment");

            // ⭐ เก็บทั้งชื่อคณะ และ FK
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

            // ⭐ บันทึกความสัมพันธ์กับ custom_keywords
            saveCustomKeywordLinks(a, c.getText());
        }
        return inserted;
    }

    // ============================================================
    // ผู้ใช้แก้ "คณะ" เอง
    // ============================================================
    @PutMapping("/faculty/update/{id}")
    public Map<String, Object> updateFaculty(
            @PathVariable String id,
            @RequestBody Map<String, String> body
    ) {
        String newFaculty = body.get("faculty");

        return repo.findById(id)
                .map(a -> {
                    a.setFaculty(newFaculty);  // เซ็ตคณะใหม่ที่ผู้ใช้เลือก
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

    //ค้นหาตามid
    @GetMapping("/{id}")
    public Map<String, Object> getAnalysisById(@PathVariable String id) {

        return repo.findById(id)
                .map(r -> {
                    String finalLabel = customKeywordService.applyCustomSentiment(
                            r.getText(),
                            r.getSentiment()
                    );

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

    // ============================================================
    // Explainability: อธิบายว่าทำไมโพสต์นี้ได้ label แบบนี้
    // ============================================================
    @GetMapping("/{id}/explain")
    public Map<String, Object> explain(@PathVariable String id) {

        return repo.findById(id)
                .map(a -> {
                    // 1) ใช้ service เดิมคำนวณ final label อีกครั้ง (ตาม custom keyword ปัจจุบัน)
                    String finalLabel = customKeywordService.applyCustomSentiment(
                            a.getText(),
                            a.getSentiment()
                    );

                    // 2) หา custom keywords ที่ติดอยู่ในโพสต์นี้
                    var matched = customKeywordService.getMatchedKeywords(a.getText());

                    List<Map<String, Object>> matchedKeywords = new ArrayList<>();
                    for (var k : matched) {
                        matchedKeywords.add(Map.of(
                                "id", k.getId(),
                                "keyword", k.getKeyword(),
                                "sentiment", k.getSentiment()
                        ));
                    }

                    // 3) เตรียม response
                    Map<String, Object> res = new HashMap<>();
                    res.put("id", a.getId());
                    res.put("text", a.getText());

                    // label ดิบจากโมเดล
                    res.put("modelLabel", a.getSentiment());
                    // label สุดท้าย (หลัง custom keyword)
                    res.put("finalLabel", finalLabel);

                    // คะแนนความมั่นใจ ถ้าไม่มีให้เป็น null
                    res.put("sentimentScore", a.getSentimentScore());

                    // ข้อมูลคณะจากแถว analysis
                    res.put("faculty", a.getFaculty());

                    // รายการ custom keyword ที่มีผล
                    res.put("matchedKeywords", matchedKeywords);

                    return res;
                })
                .orElseGet(() -> Map.of(
                        "status", "error",
                        "message", "ไม่พบ ID นี้ในฐานข้อมูล",
                        "id", id
                ));
    }


    // ============================================================
    // วิเคราะห์เฉพาะโพสต์ / คอมเมนต์ Pantip ที่ "เพิ่งเพิ่มใหม่"
    // (คือยังไม่มี record ในตาราง analysis)
    // ============================================================
    @PostMapping("/pantip/scan-new")
    @Transactional
    public Map<String, Object> analyzeNewPantip() {

        int newPosts    = analyzePantipPosts();      // ใช้ helper เดิม
        int newComments = analyzePantipComments();   // ใช้ helper เดิม

        return Map.of(
                "status", "ok",
                "newPosts", newPosts,
                "newComments", newComments
        );
    }

}
