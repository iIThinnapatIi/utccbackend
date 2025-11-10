package com.example.backend1.llm;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.DefaultUriBuilderFactory;

import java.util.HashMap;
import java.util.Map;

@Service
public class LlmService {

    private static final Logger log = LoggerFactory.getLogger(LlmService.class);

    private final RestClient rest = RestClient.builder()
            .baseUrl("http://localhost:11434/api")
            .uriBuilderFactory(new DefaultUriBuilderFactory("http://localhost:11434/api"))
            .build();

    /** ค่าเริ่มต้น */
    public String ask(String prompt) {
        return ask(prompt, "qwen2.5:7b-instruct", 0.7, 256);
    }

    /** เรียก Ollama /generate */
    public String ask(String prompt, String model, Double temperature, Integer maxTokens) {
        if (model == null || model.isBlank()) model = "qwen2.5:7b-instruct";
        if (temperature == null) temperature = 0.7;
        if (maxTokens == null) maxTokens = 256;

        try {
            Map<String, Object> options = new HashMap<>();
            options.put("temperature", temperature);
            options.put("num_predict", maxTokens);

            Map<String, Object> body = new HashMap<>();
            body.put("model", model);
            body.put("prompt", prompt);
            body.put("stream", false);
            body.put("options", options);

            Map<?, ?> res = rest.post()
                    .uri("/generate")
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            Object response = (res != null) ? res.get("response") : null;
            return (response != null) ? response.toString() : "(empty)";
        } catch (Exception e) {
            log.error("Error calling Ollama /generate", e);
            return "เกิดข้อผิดพลาดจาก Ollama: " + e.getMessage();
        }
    }
}
