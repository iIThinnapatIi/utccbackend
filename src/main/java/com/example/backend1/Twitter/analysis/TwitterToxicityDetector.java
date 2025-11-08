package com.example.backend1.Twitter.analysis;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class TwitterToxicityDetector {

    private static final Set<String> TOXIC = Set.of(
            "เหี้ย","สัส","ควาย","ไอ้","อี","เฮงซวย","กาก","โง่",
            "fuck","bitch","idiot","stupid","trash","dumb"
    );

    public double toxicityScore(String text) {
        String t = (text == null) ? "" : text.toLowerCase();
        long hits = TOXIC.stream().filter(t::contains).count();
        if (hits == 0) return 0.05;
        return Math.min(1.0, 0.5 + hits * 0.2);
    }

    public double hateSpeechScore(String text) {
        return 0.05; // baseline
    }
}
