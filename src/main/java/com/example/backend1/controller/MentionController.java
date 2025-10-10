package com.example.backend1.controller;

import com.example.backend1.model.AnalyzedTweet;
import com.example.backend1.service.AnalysisService;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = {"http://localhost:5173","http://localhost:3000"}, allowCredentials = "true")
public class MentionController {

    private final AnalysisService analysisService;
    public MentionController(AnalysisService analysisService) { this.analysisService = analysisService; }

    @GetMapping("/mentions")
    public Map<String, Object> mentions(
            @RequestParam(defaultValue = "") String q,
            @RequestParam(required = false) String sentiment,
            @RequestParam(required = false) Boolean adultOnly,
            @RequestParam(required = false) String faculty,
            // params อื่น ๆ รับไว้เฉย ๆ
            @RequestParam(defaultValue = "twitter,pantip") String sources,
            @RequestParam(required = false) List<String> topics,
            @RequestParam(required = false, name = "date_from") LocalDate dateFrom,
            @RequestParam(required = false, name = "date_to") LocalDate dateTo,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20", name = "page_size") int pageSize
    ) {
        // โหลด mock + วิเคราะห์
        var all = analysisService.searchMock(q);

        // กรอง
        var filtered = all.stream()
                .filter(t -> sentiment == null || sentiment.isBlank() || sentiment.equalsIgnoreCase(t.sentiment))
                .filter(t -> adultOnly == null || !adultOnly || t.adultFlag)
                .filter(t -> faculty == null || faculty.isBlank() || (t.faculties != null && t.faculties.contains(faculty)))
                .collect(Collectors.toList());

        // สรุป buckets
        Map<String, Long> bySent = filtered.stream().collect(Collectors.groupingBy(t -> t.sentiment, LinkedHashMap::new, Collectors.counting()));
        long adultCount = filtered.stream().filter(t -> t.adultFlag).count();
        Map<String, Long> byFaculty = new LinkedHashMap<>();
        filtered.forEach(t -> { if (t.faculties != null) t.faculties.forEach(f -> byFaculty.merge(f, 1L, Long::sum)); });

        // page
        int from = Math.max(0, (page - 1) * pageSize);
        int to = Math.min(filtered.size(), from + pageSize);
        var pageItems = from >= to ? List.<AnalyzedTweet>of() : filtered.subList(from, to);

        return Map.of(
                "query", q,
                "size", filtered.size(),
                "buckets", Map.of(
                        "sentiment", bySent,
                        "adult", adultCount,
                        "faculty", byFaculty
                ),
                "items", pageItems,
                "page", page,
                "page_size", pageSize,
                "total", filtered.size()
        );
    }
}
