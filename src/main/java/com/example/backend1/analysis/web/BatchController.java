// src/main/java/com/example/backend1/analysis/web/BatchController.java
package com.example.backend1.analysis.web;

import com.example.backend1.analysis.service.TyphoonBatchService;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/internal/batch")
public class BatchController {
    private final TyphoonBatchService svc;
    private static final String TOKEN = "CHANGE_ME_SECRET"; // เปลี่ยนเอง

    public BatchController(TyphoonBatchService svc) { this.svc = svc; }

    private void check(String token) {
        if (!TOKEN.equals(token)) throw new RuntimeException("unauthorized");
    }

    @PostMapping("/tweets")
    public Map<String,Object> runTweets(@RequestHeader("X-Token") String token) {
        check(token);
        int n = svc.analyzeTweets();
        return Map.of("inserted", n);
    }

    @PostMapping("/pantip")
    public Map<String,Object> runPantip(@RequestHeader("X-Token") String token) {
        check(token);
        int n = svc.analyzePantip();
        return Map.of("inserted", n);
    }
}
