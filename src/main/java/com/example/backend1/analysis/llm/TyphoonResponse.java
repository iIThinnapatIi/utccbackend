package com.example.backend1.analysis.llm;

import java.util.List;

public record TyphoonResponse(List<Choice> choices) {
    public record Choice(Message message) {
        public record Message(String role, String content) {}
    }
}
