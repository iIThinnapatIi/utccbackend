package com.example.backend1.analysis.web;

import com.example.backend1.analysis.service.TyphoonBatchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/typhoon")
public class TyphoonController {

    private final TyphoonBatchService typhoon;

    @Autowired
    public TyphoonController(TyphoonBatchService typhoon) {
        this.typhoon = typhoon;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("ok", "typhoon alive");
    }

    @PostMapping("/analyze/twitter")
    public Map<String, String> analyzeTwitter() {
        typhoon.analyzeTweets();
        return Map.of("status", "done", "target", "twitter");
    }

    @PostMapping("/analyze/pantip")
    public Map<String, String> analyzePantip() {
        typhoon.analyzePantip();
        return Map.of("status", "done", "target", "pantip");
    }
}
