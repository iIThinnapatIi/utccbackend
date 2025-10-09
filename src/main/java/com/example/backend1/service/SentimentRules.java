package com.example.backend1.service;

import com.example.backend1.model.Sentiment; // แก้แพ็กเกจตามที่โปรเจกต์คุณเก็บ enum นี้ไว้
import org.springframework.stereotype.Service;

import java.util.Locale;
import java.util.Set;

/**
 * กฎประมาณค่าอารมณ์แบบ heuristic ไม่พึ่ง config ภายนอก
 * ให้คอมไพล์ผ่านและใช้งานได้ทันที
 */
@Service
public class SentimentRules {

    // คำเชิงลบ/เชิงบวกเบื้องต้น (ปรับเพิ่มได้)
    private static final Set<String> NEG_WORDS = Set.of(
            "แย่","ห่วย","เกลียด","โกง","แพง","ช้า","พัง","เสีย","ล้มเหลว","ไร้มาตรฐาน","ไม่ดี","หลอก","ดราม่า","เถื่อน","น่าเกลียด"
    );
    private static final Set<String> POS_WORDS = Set.of(
            "ดี","เยี่ยม","ที่สุด","ประทับใจ","ชอบ","รัก","คุ้มค่า","แนะนำ","สุดยอด","ปลื้ม","ถูก","ไว","โปร","น่ารัก","เวิร์ค"
    );
    private static final Set<String> NEGATION = Set.of("ไม่","มิ","ไม่มี","ไม่เคย");

    public SentimentRules() {
        // ไม่ใช้ cfg/constructor injection เพื่อให้ build ผ่านก่อน
    }

    /** ประมาณค่า sentiment: positive | negative | neutral */
    public Sentiment infer(String text) {
        if (text == null || text.isBlank()) return Sentiment.neutral;

        // ตัดสัญลักษณ์ให้เหลือคำ
        String clean = text.replaceAll("[^\\p{L}\\p{N}\\s]", " ");
        String[] toks = clean.split("\\s+");

        int score = 0;
        for (int i = 0; i < toks.length; i++) {
            String t = toks[i].toLowerCase(Locale.ROOT);
            boolean negated = (i > 0 && NEGATION.contains(toks[i - 1]));
            if (POS_WORDS.contains(t)) score += negated ? -1 : 1;
            if (NEG_WORDS.contains(t)) score += negated ? 1 : -1;
        }

        if (score > 0) return Sentiment.positive;
        if (score < 0) return Sentiment.negative;
        return Sentiment.neutral;
    }
}
