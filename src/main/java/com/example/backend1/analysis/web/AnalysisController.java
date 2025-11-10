// src/main/java/com/example/backend1/analysis/web/AnalysisController.java
package com.example.backend1.analysis.web;

import com.example.backend1.analysis.repo.AnalysisViewRepository;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping({"/api/alerts", "/analysis/alerts"})
public class AnalysisController {

    private final AnalysisViewRepository repo;

    public AnalysisController(AnalysisViewRepository repo) {
        this.repo = repo;
    }
    @PostMapping("/scan")
    public Map<String,Object> scan() {
        return Map.of("status","ok","message","scan simulated");
    }

    @PostMapping("/test")
    public Map<String,Object> test() {
        return Map.of("status","ok","message","test email simulated");
    }

    @GetMapping("/summary")
    public List<Map<String,Object>> summary() {
        return repo.sentimentOverview();
    }

    @GetMapping("/trend/daily")
    public List<Map<String,Object>> trendDaily() {
        return repo.trendDaily();
    }

    @GetMapping("/top-faculties")
    public List<Map<String,Object>> topFaculties() {
        return repo.topFaculties();
    }

    @GetMapping("/latest")
    public Map<String,Object> latest(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        int p = Math.max(page, 1);
        int s = Math.max(size, 1);
        int offset = (p - 1) * s;

        List<Map<String,Object>> rows = repo.latestMentions(offset, s);
        long total = repo.latestMentionsCount();

        Map<String,Object> out = new HashMap<>();
        out.put("items", rows);
        out.put("total", total);
        out.put("page", p);
        out.put("size", s);
        return out;
    }
}
