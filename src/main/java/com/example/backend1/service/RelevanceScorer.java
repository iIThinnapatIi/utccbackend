package com.example.backend1.service;

import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class RelevanceScorer {
    private final KeywordConfig cfg;

    public double score(String textNorm) {
        String t = " " + textNorm + " ";
        int hits = 0;
        for (String w : cfg.utccWhitelist) {
            if (t.contains(" " + w + " ")) hits++;
        }
        int penalty = 0;
        for (String b : cfg.utccBlacklist) {
            if (t.contains(" " + b + " ")) penalty++;
        }
        double base = Math.min(1.0, hits * 0.4);
        base -= Math.min(0.5, penalty * 0.3);
        base = Math.max(0.0, Math.min(1.0, base));
        return base;
    }
}
