package com.example.backend1.CustomKeywords;

import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class CustomKeywordService {

    private final CustomKeywordRepo repo;
    private final CustomKeywordHistoryRepo historyRepo;   // ⭐ เพิ่ม

    public CustomKeywordService(CustomKeywordRepo repo,
                                CustomKeywordHistoryRepo historyRepo) {
        this.repo = repo;
        this.historyRepo = historyRepo;
    }

    public List<CustomKeyword> getAllKeywords() {
        return repo.findAll();
    }

    /**
     * ปรับ sentiment สุดท้ายจาก
     *  - modelSentiment = ผลจาก ONNX หรือค่าที่เก็บใน DB
     *  - text = เนื้อหาโพสต์
     *  - lexicon จากตาราง custom_keywords
     *
     * (เวอร์ชันเดิม - ยังอยู่เหมือนเดิม)
     */
    public String applyCustomSentiment(String text, String modelSentiment) {
        if (text == null || text.isEmpty()) {
            return modelSentiment;
        }

        String normText = text.toLowerCase();

        List<CustomKeyword> all = repo.findAll();
        int posScore = 0;
        int negScore = 0;

        for (CustomKeyword k : all) {
            if (k.getKeyword() == null || k.getKeyword().isEmpty()) continue;

            String kw = k.getKeyword().toLowerCase();
            if (!normText.contains(kw)) continue;   // ถ้าไม่มีคำนี้ในโพสต์ ให้ข้าม

            String s = k.getSentiment();
            if (s == null) continue;

            String normSent = s.toLowerCase();
            if (normSent.equals("positive") || normSent.equals("pos")) {
                posScore++;
            } else if (normSent.equals("negative") || normSent.equals("neg")) {
                negScore++;
            }
        }

        // ถ้าไม่เจอ custom keyword เลย   ใช้ค่าจากโมเดลตามเดิม
        if (posScore == 0 && negScore == 0) {
            return modelSentiment;
        }

        // ถ้า keyword ฝั่งไหนมากกว่า  ใช้ฝั่งนั้น
        if (posScore > negScore) {
            return "positive";
        } else if (negScore > posScore) {
            return "negative";
        } else {
            // คะแนนเท่ากัน  ให้เชื่อโมเดลเดิม
            return modelSentiment;
        }
    }

    // ===============================================================
    // 🚀 เวอร์ชันใหม่ ใช้กับการบันทึก analysis_custom_keyword
    // ===============================================================

    /**
     * applySentiment เวอร์ชันใหม่ ที่รองรับการส่ง analysisId เข้าไป
     * ใช้ตอนบันทึกผลสุดท้ายลง social_analysis
     */
    public String applyCustomSentiment(String analysisId, String text, String modelSentiment) {
        return applyCustomSentiment(text, modelSentiment);
    }

    /**
     * คืนค่า keyword_id ทั้งหมดที่ match กับข้อความนี้
     * สำหรับบันทึกลงตาราง analysis_custom_keyword
     */
    public List<Long> getMatchedKeywordIds(String text) {
        List<Long> ids = new ArrayList<>();
        if (text == null || text.isEmpty()) return ids;

        String normText = text.toLowerCase();
        List<CustomKeyword> all = repo.findAll();

        for (CustomKeyword k : all) {
            if (k.getKeyword() == null || k.getKeyword().isEmpty()) continue;

            String kw = k.getKeyword().toLowerCase();

            if (normText.contains(kw)) {
                ids.add(k.getId());  // <-- คืนค่า keyword_id
            }
        }
        return ids;
    }

    // ===============================================================
    // ⭐ ส่วนใหม่: ใช้ตอนผู้ใช้ "แก้ sentiment" ของ keyword
    // ===============================================================
    public CustomKeyword updateSentimentWithHistory(Long keywordId,
                                                    String newSentiment,
                                                    Long userId) {

        CustomKeyword kw = repo.findById(keywordId)
                .orElseThrow(() -> new RuntimeException("Keyword not found: " + keywordId));

        String oldSent = kw.getSentiment();
        kw.setSentiment(newSentiment);
        CustomKeyword saved = repo.save(kw);

        // บันทึกลง custom_keyword_history
        CustomKeywordHistory h = new CustomKeywordHistory();
        h.setKeyword(saved);
        h.setUserId(userId);              // ตอนนี้จะส่ง 1 เข้าไปก่อน
        h.setOldSentiment(oldSent);
        h.setNewSentiment(newSentiment);
        h.setChangedAt(LocalDateTime.now());

        historyRepo.save(h);

        return saved;
    }

    /**
     * คืนลิสต์ CustomKeyword ที่ match กับข้อความนี้
     * ใช้สำหรับอธิบายว่าโพสต์นี้โดนคำไหนบ้าง
     */
    public List<CustomKeyword> getMatchedKeywords(String text) {
        List<CustomKeyword> result = new ArrayList<>();
        if (text == null || text.isEmpty()) return result;

        String normText = text.toLowerCase();
        List<CustomKeyword> all = repo.findAll();

        for (CustomKeyword k : all) {
            if (k.getKeyword() == null || k.getKeyword().isEmpty()) continue;
            String kw = k.getKeyword().toLowerCase();

            if (normText.contains(kw)) {
                result.add(k);
            }
        }
        return result;
    }
}
