package com.example.backend1.Twitter.analysis;

import com.example.backend1.Twitter.dto.TweetAnalysisResult;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.stream.Collectors;

@RestController
// รองรับหลาย base paths รวมถึงที่หน้าเว็บใช้
@RequestMapping({"/api/twitter/analysis", "/api/analysis", "/analysis"})
public class TwitterAnalysisController {

    private final TwitterAnalysisService service;
    private final TweetAnalysisRepository repo;

    public TwitterAnalysisController(TwitterAnalysisService service, TweetAnalysisRepository repo) {
        this.service = service;
        this.repo = repo;
    }

    // รันวิเคราะห์ใหม่ทั้งหมดที่ pending
    @PostMapping("/run")
    public String runAll() {
        int n = service.analyzeAllPending();
        return "Analyzed " + n + " tweets.";
    }

    // เดิม: GET ที่ base (เช่น /api/analysis)
    @GetMapping
    public List<TweetAnalysisResult> list() {
        return repo.findAll().stream().map(service::toDto).collect(Collectors.toList());
    }

    // ใหม่: ให้ตรงกับหน้าบ้านที่เรียก /analysis/all (และ /api/analysis/all)
    @GetMapping("/all")
    public List<TweetAnalysisResult> listAll() {
        return repo.findAll().stream().map(service::toDto).collect(Collectors.toList());
    }

    // ดึงผลวิเคราะห์ตาม tweetId
    @GetMapping("/{tweetId}")
    public TweetAnalysisResult byTweetId(@PathVariable String tweetId) {
        TweetAnalysis entity = repo.findByTweetId(tweetId)
                .orElseThrow(() -> new ResponseStatusException(
                        HttpStatus.NOT_FOUND, "No analysis for tweetId " + tweetId));
        return service.toDto(entity);
    }

    // re-process ตาม tweetId
    @PostMapping("/{tweetId}/reprocess")
    public String reprocess(@PathVariable String tweetId) {
        service.reanalyze(tweetId);
        return "Reprocessed tweetId " + tweetId;
    }
}
