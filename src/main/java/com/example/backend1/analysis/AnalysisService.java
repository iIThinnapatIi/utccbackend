package com.example.backend1.analysis;

import com.example.backend1.analysis.dto.AnalyzeTextRequest;
import com.example.backend1.analysis.dto.MonthlyAggregateResponse;
import com.example.backend1.analysis.dto.SentimentPostTypeResponse;
import com.example.backend1.analysis.llm.TyphoonClient;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AnalysisService {

    private final TyphoonClient typhoon;
    private final TwAnalysisRepository repo;
    private final ObjectMapper om = new ObjectMapper();

    @Transactional
    public TwAnalysis analyzeAndSave(AnalyzeTextRequest req) {
        List<String> inc = req.getIncludeKeywords() == null ? List.of() : req.getIncludeKeywords();
        List<String> exc = req.getExcludeKeywords() == null ? List.of() : req.getExcludeKeywords();

        Map<String, Object> out = typhoon.analyze(req.getText(), inc, exc);

        TwAnalysis a = new TwAnalysis();
        a.setSourceId(req.getSourceId());
        a.setApp(req.getApp());
        a.setSentiment((String) out.get("sentiment"));
        a.setPostType((String) out.get("post_type"));
        a.setToxicity(num(out.get("toxicity")));
        a.setSummary((String) out.get("summary"));
        a.setLanguage((String) out.get("language"));
        a.setVersion((String) out.get("version"));

        // topics
        try { a.setTopicsJson(om.writeValueAsString(out.get("topics"))); } catch (Exception ignored) {}

        // emotions
        Map<String, Object> emo = out.get("emotions") instanceof Map<?,?> m
                ? (Map<String, Object>) m : Map.of();
        a.setJoy(num(emo.get("joy")));
        a.setAnger(num(emo.get("anger")));
        a.setSadness(num(emo.get("sadness")));
        a.setFear(num(emo.get("fear")));
        a.setSurprise(num(emo.get("surprise")));

        return repo.save(a);
    }

    public MonthlyAggregateResponse aggregateMonthly(String app, LocalDate from, LocalDate to) {
        LocalDateTime f = from.atStartOfDay();
        LocalDateTime t = to.plusDays(1).atStartOfDay().minusSeconds(1);
        var rows = repo.aggregateMonthly(app, f, t);
        var list = rows.stream().map(r ->
                new MonthlyAggregateResponse.Row(
                        (String) r[0],
                        ((Number) r[1]).longValue(),
                        ((Number) r[2]).longValue(),
                        ((Number) r[3]).longValue(),
                        ((Number) r[4]).longValue()
                )
        ).toList();

        var res = new MonthlyAggregateResponse();
        res.setByMonth(list);
        return res;
    }

    public SentimentPostTypeResponse sentimentByPostType(String app, LocalDate from, LocalDate to) {
        LocalDateTime f = from.atStartOfDay();
        LocalDateTime t = to.plusDays(1).atStartOfDay().minusSeconds(1);
        var rows = repo.sentimentByPostType(app, f, t);
        var list = rows.stream().map(r ->
                new SentimentPostTypeResponse.Row(
                        (String) r[0], (String) r[1], ((Number) r[2]).longValue()
                )
        ).toList();

        var res = new SentimentPostTypeResponse();
        res.setMatrix(list);
        return res;
    }

    private Double num(Object v) { return v == null ? null : ((Number) v).doubleValue(); }
}
