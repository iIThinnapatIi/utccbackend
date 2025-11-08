package com.example.backend1.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.List;
import java.util.Optional;

public class SettingsMapper {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public SettingsDto toDto(UserSettings e) {
        SettingsDto d = new SettingsDto();
        d.setTheme(e.getTheme());
        d.setNotificationsEnabled(e.isNotificationsEnabled());
        d.setNegativeThreshold(e.getNegativeThreshold());
        d.setAnalysisScope(e.getAnalysisScope());
        try {
            List<String> src = objectMapper.readValue(
                    Optional.ofNullable(e.getSourcesJson()).orElse("[]"),
                    new TypeReference<List<String>>() {});
            d.setSources(src);
        } catch (Exception ex) {
            d.setSources(List.of());
        }
        return d;
    }

    public void apply(SettingsDto d, UserSettings e) {
        if (d.getTheme() != null) e.setTheme(d.getTheme());
        if (d.getNotificationsEnabled() != null) e.setNotificationsEnabled(d.getNotificationsEnabled());
        if (d.getNegativeThreshold() != null) e.setNegativeThreshold(d.getNegativeThreshold());
        if (d.getAnalysisScope() != null) e.setAnalysisScope(d.getAnalysisScope());
        try {
            e.setSourcesJson(objectMapper.writeValueAsString(
                    Optional.ofNullable(d.getSources()).orElse(List.of())));
        } catch (Exception ex) {
            e.setSourcesJson("[]");
        }
    }
}
