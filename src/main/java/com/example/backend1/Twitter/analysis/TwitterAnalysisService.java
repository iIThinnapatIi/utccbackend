package com.example.backend1.Twitter.analysis;

import com.example.backend1.Twitter.Tweet;
import com.example.backend1.Twitter.TweetRepository;
import com.example.backend1.Twitter.dto.TweetAnalysisResult;
import com.example.backend1.sentiment.SentimentClient;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.example.backend1.sentiment.SentimentClient;
import com.example.backend1.sentiment.dto.AnalyzeResponse;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Service
public class TwitterAnalysisService {

    private final TweetRepository tweetRepository;
    private final TweetAnalysisRepository analysisRepository;

    // rule-based เดิม (เก็บไว้เป็น fallback)
    private final TwitterSentimentAnalyzer sentimentAnalyzer;

    private final TwitterNSFWAnalyzer nsfwAnalyzer;
    private final TwitterTopicClassifier topicClassifier;
    private final TwitterToxicityDetector toxicityDetector;

    // ✅ ใหม่: client เรียก Python Sentiment API
    private final SentimentClient sentimentClient;

    private static final int VERSION = 1;

    public TwitterAnalysisService(
            TweetRepository tweetRepository,
            TweetAnalysisRepository analysisRepository,
            TwitterSentimentAnalyzer sentimentAnalyzer,
            TwitterNSFWAnalyzer nsfwAnalyzer,
            TwitterTopicClassifier topicClassifier,
            TwitterToxicityDetector toxicityDetector,
            SentimentClient sentimentClient
    ) {
        this.tweetRepository = tweetRepository;
        this.analysisRepository = analysisRepository;
        this.sentimentAnalyzer = sentimentAnalyzer; // ใช้เป็น fallback
        this.nsfwAnalyzer = nsfwAnalyzer;
        this.topicClassifier = topicClassifier;
        this.toxicityDetector = toxicityDetector;
        this.sentimentClient = sentimentClient;     // ✅ ฉีดเข้ามา
    }

    @Transactional
    public int analyzeAllPending() {
        List<Tweet> tweets = tweetRepository.findAll();
        int processed = 0;
        for (Tweet tw : tweets) {
            String tid = String.valueOf(tw.getId());
            if (analysisRepository.findByTweetId(tid).isPresent()) continue;
            saveAnalysis(tw);
            processed++;
        }
        return processed;
    }

    @Transactional
    public void reanalyze(String tweetId) {
        Tweet tw = tweetRepository.findById(tweetId)
                .orElseThrow(() -> new IllegalArgumentException("Tweet not found: " + tweetId));
        analysisRepository.findByTweetId(tweetId).ifPresent(analysisRepository::delete);
        saveAnalysis(tw);
    }

    private void saveAnalysis(Tweet tw) {
        String text = tw.getText() == null ? "" : tw.getText().trim();

        // ตรวจภาษาแบบเร็ว (ไทย/อังกฤษ)
        String lang = text.codePoints().anyMatch(cp -> cp >= 0x0E00 && cp <= 0x0E7F) ? "th" : "en";

        // ✅ ใช้ AI ก่อน ถ้า error ค่อย fallback ไป rule-based
        String sentLabel;
        double sentScore;
        try {
            AnalyzeResponse ai = sentimentClient.analyze(text); // เรียก FastAPI
            sentLabel = ai.getLabel();      // "pos" | "neu" | "neg"
            sentScore = ai.getScore();      // 0..1
        } catch (Exception ex) {
            var rb = sentimentAnalyzer.classify(text); // fallback
            sentLabel = rb.label();
            sentScore = rb.score();
        }

        var nsfw = nsfwAnalyzer.classify(text);
        double tox = toxicityDetector.toxicityScore(text);
        double hate = toxicityDetector.hateSpeechScore(text);
        String faculty = topicClassifier.detectFaculty(text);
        var topics = topicClassifier.detectTopics(text);

        double confidence = average(sentScore, 1.0 - (tox * 0.3), 1.0 - (hate * 0.3));

        TweetAnalysis entity = new TweetAnalysis();
        entity.setTweetId(String.valueOf(tw.getId()));
        entity.setLang(lang);
        entity.setSentimentLabel(sentLabel);
        entity.setSentimentScore(bd(sentScore));
        entity.setNsfwLabel(nsfw.label());
        entity.setNsfwScore(bd(nsfw.score()));
        entity.setToxicityScore(bd(tox));
        entity.setHateSpeechScore(bd(hate));
        entity.setFaculty(faculty);
        entity.setTopicsJson(String.join(",", topics));
        entity.setConfidenceOverall(bd(confidence));
        entity.setVersion(VERSION);
        entity.setAnalyzedAt(LocalDateTime.now());

        analysisRepository.save(entity);
    }

    private static BigDecimal bd(double x) {
        // จำกัดสเกล 4 ตำแหน่งให้สอดคล้องกับ DECIMAL(6,4)
        return BigDecimal.valueOf(x).setScale(4, java.math.RoundingMode.HALF_UP);
    }

    private double average(double... xs) {
        double sum = 0;
        for (double x : xs) sum += x;
        return xs.length == 0 ? 0.0 : sum / xs.length;
    }

    public TweetAnalysisResult toDto(TweetAnalysis e) {
        TweetAnalysisResult dto = new TweetAnalysisResult();
        dto.setTweetId(e.getTweetId());
        dto.setLang(e.getLang());
        dto.setSentimentLabel(e.getSentimentLabel());
        dto.setSentimentScore(nz(e.getSentimentScore()));
        dto.setNsfwLabel(e.getNsfwLabel());
        dto.setNsfwScore(nz(e.getNsfwScore()));
        dto.setToxicityScore(nz(e.getToxicityScore()));
        dto.setHateSpeechScore(nz(e.getHateSpeechScore()));
        dto.setFaculty(e.getFaculty());
        dto.setTopics(
                (e.getTopicsJson() == null || e.getTopicsJson().isBlank())
                        ? List.of("อื่นๆ")
                        : Arrays.asList(e.getTopicsJson().split(","))
        );
        dto.setConfidenceOverall(nz(e.getConfidenceOverall()));
        dto.setVersion(e.getVersion() == null ? VERSION : e.getVersion());
        dto.setAnalyzedAt(e.getAnalyzedAt());
        return dto;
    }

    private double nz(BigDecimal bd) {
        return bd == null ? 0.0 : bd.doubleValue();
    }
}
