package com.example.backend1.controller;

import org.springframework.web.bind.annotation.*;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/api/mentions")
@RequiredArgsConstructor
public class MentionController {
    private final JdbcTemplate jdbc; // ใช้ง่าย รวดเร็ว (ต่อไปค่อยเปลี่ยนเป็น JPA Spec ได้)

    @GetMapping
    public Map<String,Object> search(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(defaultValue = "twitter,pantip") String sources,
            @RequestParam(required = false) List<String> faculty,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false) List<String> sentiment,
            @RequestParam(required = false) LocalDate date_from,
            @RequestParam(required = false) LocalDate date_to,
            @RequestParam(defaultValue = "recent") String sort,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int page_size
    ) {
        // เวอร์ชันเริ่มต้น: คืนข้อมูลว่าง + summary โครงสร้างถูกต้อง
        // ภายหลังจะใส่ SQL builder เพื่อรองรับฟิลเตอร์ทั้งหมด
        return Map.of(
                "items", List.of(),
                "summary", Map.of(
                        "count_by_source", Map.of("twitter",0, "pantip",0),
                        "count_by_faculty", Map.of(),
                        "count_by_sentiment", Map.of("positive",0, "neutral",0, "negative",0)
                ),
                "page", page,
                "page_size", page_size,
                "total", 0
        );
    }
}