package com.example.backend1.Pantip;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class PantipScheduler {

    private final PantipScraperService scraper;

    public PantipScheduler(PantipScraperService scraper) {
        this.scraper = scraper;
    }

    //อันนี้ไม่เกี่ยว อันนี้ดึงอัตนโม
    // ทุกวันเวลา 00:00 น. (โซนเวลาไทย)
    @Scheduled(cron = "0 0 0 * * *", zone = "Asia/Bangkok")
    public void runDaily() {

        List<String> keywords = List.of(
                "UTCC",
                "มหาวิทยาลัยหอการค้าไทย",
                "หอการค้า",
                "เด็กหอการค้า",
                "รีวิวหอการค้า"
        );

        for (String kw : keywords) {

            scraper.scrapePantip(kw);

            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
        }
    }
}
