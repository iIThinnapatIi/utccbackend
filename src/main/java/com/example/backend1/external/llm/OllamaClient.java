package com.example.backend1.external.llm;

import com.fasterxml.jackson.annotation.JsonProperty;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class OllamaClient {

    private final RestTemplate rest = new RestTemplate();
    // ถ้าใช้ค่าเริ่มต้นของ Ollama ให้เป็น 11434
    private final String baseUrl = "http://localhost:11434";

    public String generate(String model, String prompt, Double temperature, Integer numCtx) {
        GenerateRequest req = new GenerateRequest();
        req.model = model;
        req.prompt = prompt;
        req.stream = false; // เอาคำตอบทีเดียว
        req.options = new GenerateOptions();
        req.options.temperature = temperature;
        req.options.num_ctx = numCtx;

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<GenerateRequest> entity = new HttpEntity<>(req, headers);

        GenerateResponse res = rest.postForObject(
                baseUrl + "/api/generate",
                entity,
                GenerateResponse.class
        );

        return (res != null) ? res.response : null;
    }

    // ===== DTOs =====

    /** รูปแบบ request ของ /api/generate */
    public static class GenerateRequest {
        public String model;
        public String prompt;
        public boolean stream;

        @JsonProperty("options")
        public GenerateOptions options;
    }

    /** options ตามสเปคของ Ollama */
    public static class GenerateOptions {
        public Double temperature;

        @JsonProperty("num_ctx")
        public Integer num_ctx;
    }

    /** รูปแบบ response ของ /api/generate */
    public static class GenerateResponse {
        public String model;

        /** เนื้อหาคำตอบ */
        public String response;

        /** true เมื่อจบการตอบ */
        public boolean done;

        @JsonProperty("total_duration")
        public Long totalDuration;
    }
}
