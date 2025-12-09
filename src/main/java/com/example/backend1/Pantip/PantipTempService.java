package com.example.backend1.Pantip;

import com.example.backend1.Analysis.Analysis;
import com.example.backend1.Analysis.AnalysisRepository;
import com.example.backend1.Analysis.OnnxSentimentService;
import com.example.backend1.Analysis.AnalysisCustomKeyword;
import com.example.backend1.Analysis.AnalysisCustomKeywordRepo;
import com.example.backend1.CustomKeywords.CustomKeywordService;

import com.example.backend1.Faculty.Faculty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class PantipTempService {

    private final PantipScraperService scraper;
    private final OnnxSentimentService onnx;
    private final AnalysisRepository analysisRepo;

    // ⭐ เพิ่มใหม่
    private final CustomKeywordService customKeywordService;
    private final AnalysisCustomKeywordRepo ackRepo;

    // ⭐ Constructor ใหม่ (แต่คอมเมนต์เดิมไม่โดนลบ)
    public PantipTempService(
            PantipScraperService scraper,
            OnnxSentimentService onnx,
            AnalysisRepository analysisRepo,
            CustomKeywordService customKeywordService,
            AnalysisCustomKeywordRepo ackRepo
    ) {
        this.scraper = scraper;
        this.onnx = onnx;
        this.analysisRepo = analysisRepo;
        this.customKeywordService = customKeywordService; // ⭐ เพิ่ม
        this.ackRepo = ackRepo;                           // ⭐ เพิ่ม
    }

    /*
      1) ดึงโพสต์จาก "เว็บพันทิป" แบบ preview (ยังไม่บันทึก DB)
         - ใช้เมธอดของเดิม: scraper.scrapePantipTemp(keyword)
        - PantipScraperService จะเก็บโพสต์ไว้ใน tempPosts ภายในตัวมันเองด้วย
     */
    public List<PantipPost> fetchTemp(String keyword) throws Exception {
        // ดึงสดจากเว็บตาม keyword แบบ temp
        return scraper.scrapePantipTemp(keyword);
    }


    public int saveTempToSocialAnalysis() {
        // ดึงโพสต์ที่ scrape ไว้ใน temp
        List<PantipPost> tempPosts = scraper.getTemp();
        if (tempPosts == null || tempPosts.isEmpty()) {
            return 0;
        }

        int saved = 0;

        // 1) วิเคราะห์ + เซฟลง social_analysis
        for (PantipPost p : tempPosts) {
            String text = p.getContent();   // เนื้อหาโพสต์
            if (text == null || text.isBlank()) {
                continue;
            }

            // 🔹 เรียก ONNX วิเคราะห์ sentiment + faculty
            OnnxSentimentService.SentimentResult res = onnx.analyze(text);

            // เตรียม entity สำหรับตาราง social_analysis
            Analysis row = new Analysis();

            //  ใช้ UUID เป็น primary key ป้องกัน duplicate
            row.setId(UUID.randomUUID().toString());

            row.setText(text);
            row.setPlatform("pantip");

            String createdAt = p.getPostTime();
            if (createdAt == null || createdAt.isBlank()) {
                createdAt = LocalDateTime.now().toString();
            }
            row.setCreatedAt(createdAt);

            // ⭐ เก็บชื่อคณะ + FK จากผล ONNX
            String facName = res.getFacultyName() != null
                    ? res.getFacultyName()
                    : "ไม่ระบุ";
            row.setFaculty(facName);

            if (res.getFacultyId() != null) {
                Faculty fac = new Faculty();
                fac.setId(res.getFacultyId());
                row.setFacultyRef(fac);
            } else {
                row.setFacultyRef(null);
            }

            // --------------- ⭐ ใช้ Custom Keywords ปรับผล ---------------
            String finalLabel = customKeywordService.applyCustomSentiment(
                    row.getId(),            // analysisId
                    text,                   // text
                    res.getLabel()          // modelSentiment
            );

            row.setSentiment(res.getLabel());   // ผลจาก AI
            row.setFinalLabel(finalLabel);      // ผลหลังถูกแก้โดย custom keyword
            row.setSentimentScore((double) res.getScore()); // ✅ ใหม่: cast เป็น double
            // -------------------------------------------------------------

            analysisRepo.save(row);
            saved++;

            // --------------- ⭐ บันทึก keyword ที่ match ลงตารางกลาง ---------------
            List<Long> matchedKeywordIds = customKeywordService.getMatchedKeywordIds(text);

            for (Long kid : matchedKeywordIds) {
                ackRepo.save(new AnalysisCustomKeyword(row.getId(), kid));
            }
            // -----------------------------------------------------------------
        }

        scraper.saveTempToDB();
        scraper.clearTemp();

        return saved;
    }

    public void clearTemp() {
        scraper.clearTemp();
    }
}
