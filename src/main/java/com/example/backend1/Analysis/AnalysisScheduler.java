package com.example.backend1.Analysis;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class AnalysisScheduler {

    private final AnalysisController controller;

    public AnalysisScheduler(AnalysisController controller) {
        this.controller = controller;
    }

    // วิเคราะห์โพสใหม่ทุกวันหลังดึงเสร็จ
    @Scheduled(cron = "5 0 0 * * *", zone = "Asia/Bangkok")
    public void autoAnalyzePantip() {
        controller.analyzeNewPantip();
    }
}
