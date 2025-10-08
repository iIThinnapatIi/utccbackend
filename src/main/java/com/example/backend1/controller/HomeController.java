package com.example.backend1.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.OffsetDateTime;
import java.util.Map;

@RestController
public class HomeController {

    @GetMapping(value = "/", produces = "application/json;charset=UTF-8")
    public ResponseEntity<Map<String, Object>> root() {
        return ResponseEntity.ok(
                Map.of(
                        "app", "backend1",
                        "status", "OK",
                        "time", OffsetDateTime.now().toString(),
                        "hint", "ลองเรียก /api/... อื่นๆ ดู"
                )
        );
    }
}
