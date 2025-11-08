package com.example.backend1.analysis.llm;

import java.util.List;

public record TyphoonRequest(
        String model,
        List<Message> messages,
        Integer temperature,
        Integer max_tokens,
        ResponseFormat response_format
) {
    public record Message(String role, String content) {}
    public record ResponseFormat(String type) {}
}
