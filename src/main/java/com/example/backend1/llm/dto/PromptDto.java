package com.example.backend1.llm.dto;

/** payload ที่รับจาก POST /api/llm/generate */
public class PromptDto {
    private String prompt;        // ข้อความที่ให้โมเดลตอบ
    private String model;         // (optional) เช่น "qwen2.5:7b-instruct"
    private Double temperature;   // (optional) default 0.7
    private Integer maxTokens;    // (optional) เช่น 256/512

    public PromptDto() {}

    public String getPrompt() {
        return prompt;
    }
    public void setPrompt(String prompt) {
        this.prompt = prompt;
    }

    public String getModel() {
        return model;
    }
    public void setModel(String model) {
        this.model = model;
    }

    public Double getTemperature() {
        return temperature;
    }
    public void setTemperature(Double temperature) {
        this.temperature = temperature;
    }

    public Integer getMaxTokens() {
        return maxTokens;
    }
    public void setMaxTokens(Integer maxTokens) {
        this.maxTokens = maxTokens;
    }
}
