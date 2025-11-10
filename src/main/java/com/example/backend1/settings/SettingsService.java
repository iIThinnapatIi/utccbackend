package com.example.backend1.settings;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

@Service
public class SettingsService {

    private static final Logger log = LoggerFactory.getLogger(SettingsService.class);

    private final UserSettingsRepository repo;
    private static final ObjectMapper M = new ObjectMapper();

    public SettingsService(UserSettingsRepository repo) {
        this.repo = repo;
    }

    private SettingsDto toDto(UserSettings e) {
        SettingsDto d = new SettingsDto();
        d.setUpdatedAt(e.getUpdatedAt());
        d.setTheme(e.getTheme());
        d.setNotificationsEnabled(e.isNotificationsEnabled());
        d.setNegativeThreshold(e.getNegativeThreshold());
        try {
            List<String> src = M.readValue(
                    (e.getSourcesJson() == null || e.getSourcesJson().isBlank()) ? "[]" : e.getSourcesJson(),
                    new TypeReference<List<String>>() {});
            d.setSources(src);
        } catch (Exception ex) {
            log.warn("Parse sourcesJson failed: {}", ex.getMessage());
            d.setSources(Collections.emptyList());
        }
        d.setAnalysisScope(e.getAnalysisScope());
        return d;
    }

    private void apply(SettingsDto d, UserSettings e) {
        if (d.getTheme() != null)
            e.setTheme("DARK".equalsIgnoreCase(d.getTheme()) ? "DARK" : "LIGHT");
        if (d.getNotificationsEnabled() != null)
            e.setNotificationsEnabled(d.getNotificationsEnabled());
        if (d.getNegativeThreshold() != null)
            e.setNegativeThreshold(d.getNegativeThreshold());
        if (d.getSources() != null) {
            try {
                e.setSourcesJson(M.writeValueAsString(d.getSources()));
            } catch (Exception ex) {
                log.warn("Write sourcesJson failed: {}", ex.getMessage());
            }
        }
        if (d.getAnalysisScope() != null)
            e.setAnalysisScope(d.getAnalysisScope());

        e.setUpdatedAt(LocalDateTime.now());
    }

    private UserSettings getOrCreate() {
        return repo.findAll().stream().findFirst().orElseGet(() -> {
            UserSettings s = new UserSettings();
            s.setTheme("LIGHT");
            s.setNotificationsEnabled(true);
            s.setNegativeThreshold(20);
            s.setSourcesJson("[\"news\",\"forums\",\"youtube\",\"tiktok\",\"blogs\"]");
            s.setAnalysisScope("ทั้งหมด");
            s.setUpdatedAt(LocalDateTime.now());
            return repo.save(s);
        });
    }

    @Transactional(readOnly = true)
    public SettingsDto get() {
        return toDto(getOrCreate());
    }

    @Transactional
    public SettingsDto update(SettingsDto dto) {
        UserSettings s = getOrCreate();
        apply(dto, s);
        repo.save(s);
        return toDto(s);
    }
}
