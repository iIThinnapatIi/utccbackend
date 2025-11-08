package com.example.backend1.analysis.llm;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Component
public class TyphoonClient {

    private final RestClient rest;
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${typhoon.model}")
    private String model;

    public TyphoonClient(RestClient typhoonRestClient) {
        this.rest = typhoonRestClient;
    }

    public Map<String, Object> analyze(String text, List<String> include, List<String> exclude) {
        String sysPrompt = """
            คุณคือระบบวิเคราะห์ข้อความภาษาไทยสำหรับมหาวิทยาลัย UTCC
            จงตอบเป็น JSON เดียวเท่านั้น ตามสคีมาต่อไปนี้:
            {
              "sentiment": "positive|neutral|negative",
              "emotions": {"joy":0..1,"anger":0..1,"sadness":0..1,"fear":0..1,"surprise":0..1},
              "topics": ["admissions","events","academics","facilities","finance","complaint","praise"],
              "post_type": "ประกาศ|รีวิว/ชมเชย|บ่น/ร้องเรียน|ข่าวกิจกรรม|สอบถาม|อื่นๆ",
              "toxicity": 0..1,
              "summary": string,
              "language": string,
              "version": "typhoon-2.x"
            }
            คำที่ควรให้ความสำคัญ (include): %s
            คำที่ควรหลีกเลี่ยง (exclude): %s
            กฎ: ห้ามมีคำอธิบายอื่นนอกเหนือจาก JSON เดียวที่สอดคล้องกับสคีมา
            """.formatted(include, exclude);

        TyphoonRequest req = new TyphoonRequest(
                model,
                List.of(
                        new TyphoonRequest.Message("system", sysPrompt),
                        new TyphoonRequest.Message("user", text)
                ),
                0,
                512,
                new TyphoonRequest.ResponseFormat("json_object")
        );

        TyphoonResponse res = rest.post()
                .uri("/v1/chat/completions")
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(TyphoonResponse.class);

        if (res == null || res.choices() == null || res.choices().isEmpty()) {
            throw new RuntimeException("Typhoon: empty response");
        }
        String json = res.choices().get(0).message().content();
        try {
            return mapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            throw new RuntimeException("Invalid JSON from Typhoon: " + json, e);
        }
    }
}
