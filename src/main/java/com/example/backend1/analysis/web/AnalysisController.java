package com.example.backend1.analysis.web;

import com.example.backend1.analysis.dto.AnalyzeRequest;
import com.example.backend1.analysis.dto.AnalyzeResponse;
import com.example.backend1.analysis.model.LlmAnalysis;
import com.example.backend1.analysis.repo.LlmAnalysisRepository;
import com.example.backend1.analysis.service.LlmAnalyzer;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/analysis")
public class AnalysisController {

    private final LlmAnalyzer analyzer;
    private final LlmAnalysisRepository repo;

    public AnalysisController(LlmAnalyzer analyzer, LlmAnalysisRepository repo) {
        this.analyzer = analyzer;
        this.repo = repo;
    }

    @PostMapping("/llm")
    public AnalyzeResponse analyze(@RequestBody AnalyzeRequest req) {
        AnalyzeResponse result = analyzer.analyze(req);

        if (Boolean.TRUE.equals(req.getSave())) {
            LlmAnalysis e = new LlmAnalysis();
            e.setApp(req.getApp());
            e.setSource(req.getSource());
            e.setText(req.getText());
            e.setSentiment(result.getSentiment());
            e.setTopic(result.getTopic());
            e.setAnswerRaw(result.getAnswerRaw());
            e.setCreatedAt(LocalDateTime.now());
            repo.save(e);
        }
        return result;
    }
}
