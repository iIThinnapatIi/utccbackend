package com.example.backend1.Twitter.analysis;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class TwitterSentimentAnalyzer {

    private static final Set<String> POS = Set.of(
            "ดี","ชอบ","รัก","เยี่ยม","สุดยอด","ประทับใจ","โอเค","แนะนำ",
            "love","great","awesome","amazing","nice","good","recommend"
    );

    private static final Set<String> NEG = Set.of(
            "แย่","เกลียด","ห่วย","โกรธ","ผิดหวัง","กาก","ขยะ",
            "bad","terrible","awful","hate","worst","trash"
    );

    public Result classify(String text) {
        String t = (text == null) ? "" : text.toLowerCase();
        int posHit = (int) POS.stream().filter(t::contains).count();
        int negHit = (int) NEG.stream().filter(t::contains).count();

        if (posHit > negHit && posHit > 0) return new Result("pos", Math.min(1.0, 0.6 + posHit * 0.1));
        if (negHit > posHit && negHit > 0) return new Result("neg", Math.min(1.0, 0.6 + negHit * 0.1));
        return new Result("neu", 0.55);
    }

    public static final class Result {
        private final String label;
        private final double score;
        public Result(String label, double score) { this.label = label; this.score = score; }
        public String label() { return label; }
        public double score() { return score; }
    }
}
