package com.example.backend1.CustomKeywords;

import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CustomKeywordService {

    private final CustomKeywordRepo repo;

    public CustomKeywordService(CustomKeywordRepo repo) {
        this.repo = repo;
    }


    public List<CustomKeyword> getAllKeywords() {
        return repo.findAll();
    }

    /**
     * ปรับ sentiment สุดท้ายจาก
     *  - modelSentiment = ผลจาก ONNX หรือค่าที่เก็บใน DB
     *  - text = เนื้อหาโพสต์
     *  - lexicon จากตาราง custom_keywords
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
}
