package com.example.backend1.service;

import org.springframework.stereotype.Service;

@Service
public class RelevanceScorer {

    // คีย์เวิร์ดเบื้องต้นเกี่ยวกับ UTCC (ปรับเพิ่มได้ภายหลัง)
    private static final String[] KEYWORDS = {
            "utcc", "มหาวิทยาลัยหอการค้า", "หอการค้าไทย", "มหาวิทยาลัย หอการค้า"
    };

    public RelevanceScorer() {
        // ไม่ต้องพึ่ง config ภายนอก เพื่อให้คอมไพล์ผ่านก่อน
    }

    /**
     * ให้คะแนนความเกี่ยวข้อง (0..1) แบบ heuristic ง่ายๆ
     * - มีคีย์เวิร์ดตรง: บวกน้ำหนัก
     * - ความยาวข้อความช่วยให้ไม่ศูนย์ (ป้องกันโดนตัดคะแนนหมด)
     */
    public double score(String text) {
        if (text == null || text.isBlank()) return 0.0;

        String t = text.toLowerCase();
        int hits = 0;
        for (String k : KEYWORDS) {
            if (t.contains(k.toLowerCase())) hits++;
        }
        double keywordScore = Math.min(1.0, hits * 0.5);        // คีย์เวิร์ดละ 0.5
        double lenFactor    = Math.min(1.0, t.length() / 140.0); // ตามสเกลทวิต

        double s = 0.7 * keywordScore + 0.3 * lenFactor;
        if (Double.isNaN(s) || Double.isInfinite(s)) return 0.0;
        return Math.max(0.0, Math.min(1.0, s));
    }
}
