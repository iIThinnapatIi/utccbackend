package com.example.backend1.Pantip;

import com.example.backend1.Analysis.Analysis;
import com.example.backend1.Analysis.AnalysisRepository;
import com.example.backend1.Analysis.OnnxSentimentService;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;


@Service
public class PantipTempService {

    private final PantipScraperService scraper;
    private final OnnxSentimentService onnx;
    private final AnalysisRepository analysisRepo;

    public PantipTempService(
            PantipScraperService scraper,
            OnnxSentimentService onnx,
            AnalysisRepository analysisRepo
    ) {
        this.scraper = scraper;
        this.onnx = onnx;
        this.analysisRepo = analysisRepo;
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

            // เรียก ONNX วิเคราะห์ sentiment
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



            row.setSentiment(res.getLabel());
            row.setFinalLabel(res.getLabel());


            analysisRepo.save(row);
            saved++;
        }


        scraper.saveTempToDB();


        scraper.clearTemp();

        return saved;
    }


    public void clearTemp() {
        scraper.clearTemp();
    }
}