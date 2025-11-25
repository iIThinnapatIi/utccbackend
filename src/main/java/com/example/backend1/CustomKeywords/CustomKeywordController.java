package com.example.backend1.CustomKeywords;

import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/custom-keywords")
@CrossOrigin(origins = "http://localhost:5173")
public class CustomKeywordController {

    private final CustomKeywordRepo repo;

    public CustomKeywordController(CustomKeywordRepo repo) {
        this.repo = repo;
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

    // ลบคำตาม ID (ฟีเจอร์ใหม่)
    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        repo.deleteById(id);
    }
}
