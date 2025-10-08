package com.example.backend1.service;

import com.example.backend1.model.Sentiment;
import org.springframework.stereotype.Component;
import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class SentimentRules {
    private final KeywordConfig cfg;

    public Sentiment infer(String textNorm) {
        int pos = 0, neg = 0;
        for (String w : cfg.posWords) if (textNorm.contains(w)) pos++;
        for (String w : cfg.negWords) if (textNorm.contains(w)) neg++;
        if (pos == 0 && neg == 0) return Sentiment.neutral;
        if (pos > neg) return Sentiment.positive;
        if (neg > pos) return Sentiment.negative;
        return Sentiment.neutral;
    }
}