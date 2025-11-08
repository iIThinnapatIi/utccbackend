package com.example.backend1.analysis;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/analysis")
@RequiredArgsConstructor
public class AnalysisController {

    private final AnalysisService service;

    @GetMapping("/all")
    public Object all() {
        return service.getAll();
    }

    @PostMapping("/analyze")
    public Map<String, Object> analyze(@RequestParam String text) {
        AnalysisService.AnalyzeResult r = service.analyzeText(text);
        return Map.of("sentiment", r.getSentiment(), "score", r.getScore());
    }

    @PostMapping("/mock")
    public Map<String, String> generateMock() {
        service.generateMockAnalysis();
        return Map.of("status", "ok");
    }
}
