package com.example.backend1.analysis.service;

import com.example.backend1.analysis.dto.AnalyzeRequest;
import com.example.backend1.analysis.dto.AnalyzeResponse;
import com.example.backend1.llm.LlmService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class LlmAnalyzer {

    private final LlmService llm;
    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    public LlmAnalyzer(LlmService llm) {
        this.llm = llm;
    }

    public AnalyzeResponse analyze(AnalyzeRequest req) {
        AnalyzeResponse res = new AnalyzeResponse();

        // 1) พรอมต์กำหนดรูปแบบให้ LLM ตอบเป็น JSON ภาษาไทยเท่านั้น
        String prompt = """
            คุณคือนักวิเคราะห์ข้อมูลโซเชียลภาษาไทย
            วิเคราะห์ข้อความต่อไปนี้ แล้วส่งผลลัพธ์เป็น JSON เท่านั้น ห้ามมีคำอธิบายอื่นๆ
            รูปแบบ JSON:
            {
              "sentiment": "positive|neutral|negative",
              "topic": "<หัวข้อสั้นๆ 1 คำหรือวลี>",
              "summary": "<สรุปใจความสั้นๆ 1-2 ประโยค>"
            }
            ข้อความ:
            %s
            ตอบเป็นภาษาไทยเท่านั้น
            """.formatted(req.getText());

        // 2) เรียก LLM ผ่าน service ที่คุณสร้างไว้
        String answer = llm.ask(
                prompt,
                req.getModel(),
                req.getTemperature(),
                req.getMaxTokens()
        );

        // 3) แปลงผลจาก LLM เป็น JSON แล้วเก็บใน response
        try {
            JsonNode node = mapper.readTree(answer);
            res.setSentiment(node.path("sentiment").asText("unknown"));
            res.setTopic(node.path("topic").asText("unknown"));
            res.setAnswerRaw(answer);
        } catch (Exception e) {
            // ถ้าไม่ใช่ JSON ที่ parse ได้ ก็เก็บข้อความดิบไว้
            res.setSentiment("unknown");
            res.setTopic("unknown");
            res.setAnswerRaw(answer);
        }

        return res;
    }
}
