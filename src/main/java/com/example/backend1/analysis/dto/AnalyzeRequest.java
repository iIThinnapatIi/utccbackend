package com.example.backend1.analysis.dto;

public class AnalyzeRequest {
    private String text;
    private String app;
    private String source;
    private String model;
    private Double temperature;
    private Integer maxTokens;
    private Boolean save;

    public String getText() { return text; }
    public void setText(String text) { this.text = text; }

    public String getApp() { return app; }
    public void setApp(String app) { this.app = app; }

    public String getSource() { return source; }
    public void setSource(String source) { this.source = source; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public Double getTemperature() { return temperature; }
    public void setTemperature(Double temperature) { this.temperature = temperature; }

    public Integer getMaxTokens() { return maxTokens; }
    public void setMaxTokens(Integer maxTokens) { this.maxTokens = maxTokens; }

    public Boolean getSave() { return save; }
    public void setSave(Boolean save) { this.save = save; }
}
