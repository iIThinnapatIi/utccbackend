package com.example.backend1.analysis.web;

import com.example.backend1.analysis.service.TyphoonBackfillService;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/typhoon")
public class TyphoonBackfillController {

    private final TyphoonBackfillService svc;

    // ให้ Spring ฉีด Service เข้ามา
    public TyphoonBackfillController(TyphoonBackfillService svc) {
        this.svc = svc;
    }

    // รวม: เติมทั้ง Twitter + Pantip
    @PostMapping("/backfill")
    public Map<String, Object> backfillAll() {
        int tweets = svc.backfillTweets();
        int pantip = svc.backfillPantip();
        return Map.of(
                "status", "ok",
                "tweets_inserted", tweets,
                "pantip_inserted", pantip
        );
    }

    // แยก: เติมเฉพาะ Twitter
    @PostMapping("/backfill/tweets")
    public Map<String, Object> backfillTweets() {
        return Map.of("inserted", svc.backfillTweets());
    }

    // แยก: เติมเฉพาะ Pantip
    @PostMapping("/backfill/pantip")
    public Map<String, Object> backfillPantip() {
        return Map.of("inserted", svc.backfillPantip());
    }
}
