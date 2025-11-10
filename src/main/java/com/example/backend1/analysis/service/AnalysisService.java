package com.example.backend1.analysis.service;

import com.example.backend1.analysis.dto.MonthlyAggregateResponse;
import com.example.backend1.analysis.repo.TwAnalysisRepository;
import com.example.backend1.external.llm.TyphoonClient;
import com.example.backend1.external.llm.TyphoonRequest;
import com.example.backend1.external.llm.TyphoonResponse;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Service
public class AnalysisService {

    private final TwAnalysisRepository repo;
    private final TyphoonClient typhoonClient; // อาจเป็น null ได้ถ้ายังไม่ได้ประกาศ bean

    // ✅ เขียน constructor เอง แทน Lombok @RequiredArgsConstructor
    public AnalysisService(TwAnalysisRepository repo, TyphoonClient typhoonClient) {
        this.repo = repo;
        this.typhoonClient = typhoonClient;
    }

    /** สรุปรายเดือนจาก DB (ไม่พึ่ง AI) */
    public List<MonthlyAggregateResponse> monthly(String app, LocalDate from, LocalDate to) {
        LocalDateTime fromDt = from.atStartOfDay();
        LocalDateTime toDt   = to.plusDays(1).atStartOfDay();

        List<Object[]> rows = repo.aggregateMonthly(app, fromDt, toDt);
        List<MonthlyAggregateResponse> out = new ArrayList<>();

        for (Object[] r : rows) {
            out.add(new MonthlyAggregateResponse(
                    (String)  r[0],
                    ((Number) r[1]).longValue(),
                    ((Number) r[2]).longValue(),
                    ((Number) r[3]).longValue(),
                    ((Number) r[4]).longValue()
            ));
        }
        return out;
    }

    /** วิเคราะห์ข้อความเดี่ยวด้วยไต้ฝุ่น (ป้องกันกรณี typhoonClient ยังไม่ถูกตั้งค่า) */
    public TyphoonResponse analyzeText(String text, String lang) {
        if (typhoonClient == null) {
            throw new IllegalStateException("TyphoonClient bean is not available. Please configure typhoonRestClient and TyphoonClient.");
        }
        TyphoonRequest req = new TyphoonRequest();
        req.setText(text);
        req.setLang(lang == null ? "th" : lang);
        return typhoonClient.analyze(req);
    }
}
