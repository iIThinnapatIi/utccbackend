package com.example.backend1.Twitter.analysis;

import org.springframework.stereotype.Component;
import java.util.Set;

@Component
public class TwitterNSFWAnalyzer {

    private static final Set<String> EXPLICIT = Set.of(
            "18+","sex","porn","xnxx","xvideos","คอลเสียว","นู้ด","นัด","เย็ด","เสียว","หาคนสนอง"
    );
    private static final Set<String> BORDERLINE = Set.of(
            "หื่น","โลลิ","แซ่บ","เซ็กซี่","หวาบหวิว","เรท"
    );

    public Result classify(String text) {
        String t = (text == null) ? "" : text.toLowerCase();
        if (EXPLICIT.stream().anyMatch(t::contains)) return new Result("explicit", 0.90);
        if (BORDERLINE.stream().anyMatch(t::contains)) return new Result("borderline", 0.65);
        return new Result("safe", 0.05);
    }

    public static final class Result {
        private final String label;
        private final double score;
        public Result(String label, double score) { this.label = label; this.score = score; }
        public String label() { return label; }
        public double score() { return score; }
    }
}
