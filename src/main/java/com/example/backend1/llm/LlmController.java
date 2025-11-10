package com.example.backend1.llm;

import com.example.backend1.llm.dto.PromptDto;
import org.springframework.web.bind.annotation.*;
import java.util.Map;

@RestController
@RequestMapping("/api/llm")
public class LlmController {

    private final LlmService llm;

    // ✅ สร้าง constructor เอง ไม่ต้องพึ่ง Lombok
    public LlmController(LlmService llm) {
        this.llm = llm;
    }

    @GetMapping("/ping")
    public Map<String, String> ping() {
        return Map.of("ok", "llm alive");
    }

    @PostMapping("/generate")
    public Map<String, String> generate(@RequestBody PromptDto body) {
        String answer = llm.ask(
                body.getPrompt(),
                body.getModel(),
                body.getTemperature(),
                body.getMaxTokens()
        );
        return Map.of("answer", answer);
    }
}
