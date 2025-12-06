package com.example.backend1.CustomKeywords;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/custom-keywords")
@CrossOrigin(origins = "http://localhost:5173")
public class CustomKeywordController {

    private final CustomKeywordRepo repo;
    private final CustomKeywordService service;

    public CustomKeywordController(CustomKeywordRepo repo,
                                   CustomKeywordService service) {
        this.repo = repo;
        this.service = service;
    }

    @GetMapping
    public List<CustomKeyword> all() {
        return repo.findAll();
    }

    // เพิ่มคำใหม่
    @PostMapping
    public CustomKeyword add(@RequestBody CustomKeyword ck) {
        return repo.save(ck);
    }

    // ⭐ อัปเดต sentiment (ใช้ตอนเปลี่ยน dropdown บนหน้าเว็บ)
    @PutMapping("/{id}")
    public CustomKeyword update(
            @PathVariable Long id,
            @RequestBody CustomKeyword body
    ) {
        String newSentiment = body.getSentiment();

        // ตอนนี้ยังไม่มีระบบ login → ใส่ userId = 1 ไปก่อน
        Long userId = 1L;

        return service.updateSentimentWithHistory(id, newSentiment, userId);
    }

    // ลบคำตาม ID (ฟีเจอร์ใหม่)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
