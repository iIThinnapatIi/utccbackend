package com.example.backend1.mentions;

import org.springframework.web.bind.annotation.*;

@RestController("mentionsSearchController")      // ✅ ตั้งชื่อ bean ใหม่ ไม่ชนกัน
@RequestMapping("/api/mentions/search")          // ✅ ตั้ง path ใหม่ ไม่ทับกับ /api/mentions
public class MentionsController {

    @GetMapping
    public Object search(
            @RequestParam(defaultValue = "utcc") String q,
            @RequestParam(defaultValue = "30") int max
    ) {
        // TODO: ใส่ logic เดิมของคุณ
        return java.util.Map.of(
                "items", java.util.List.of(),
                "facetSentiment", java.util.Map.of(),
                "facetCategory", java.util.Map.of(),
                "facetFaculty", java.util.Map.of()
        );
    }
}
