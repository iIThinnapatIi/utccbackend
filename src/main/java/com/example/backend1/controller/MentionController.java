package com.example.backend1.controller;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.*;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173","http://localhost:3000"}, allowCredentials = "true")
public class MentionController {

    private final JdbcTemplate jdbc;

    public MentionController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    // ✅ ตอบที่ /api/mentions (ตรงกับ frontend)
    @GetMapping("/mentions")
    public Map<String, Object> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "twitter,pantip") String sources,
            @RequestParam(required = false) List<String> faculty,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false) List<String> sentiment,
            @RequestParam(required = false, name = "date_from") LocalDate dateFrom,
            @RequestParam(required = false, name = "date_to") LocalDate dateTo,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20", name = "page_size") int pageSize
    ) {
        // mock โครงสร้างมาตรฐาน
        return Map.of(
                "items", List.of(),
                "summary", Map.of(
                        "count_by_source", Map.of("twitter", 0, "pantip", 0),
                        "count_by_faculty", Map.of(),
                        "count_by_sentiment", Map.of("positive", 0, "neutral", 0, "negative", 0)
                ),
                "page", page,
                "page_size", pageSize,
                "total", 0
        );
    }

    // (ถ้าต้องการคง endpoint legacy ไว้ด้วย ให้เปิดอันล่างนี้)
    // @GetMapping("/mentions/legacy")
    // public Map<String,Object> legacy(...) { ...เหมือนด้านบน... }
}
