package com.example.backend1.service;

import com.example.backend1.model.AnalyzedTweet;
import com.example.backend1.util.CategoryClassifier;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.util.*;
import java.util.stream.StreamSupport;

@Service
public class AnalysisService {
    private final ObjectMapper mapper = new ObjectMapper();

    public List<AnalyzedTweet> searchMock(String keyword) {
        try (InputStream is = resolveMock()) {
            if (is == null) {
                System.err.println("[mock] NOT FOUND mock_tweets.json");
                return List.of();
            }
            String json = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            JsonNode root = mapper.readTree(json);

            // รองรับทั้ง {data:[...]} / {statuses:[...]} / array ตรงๆ
            JsonNode arr = root;
            if (root.has("data") && root.get("data").isArray()) arr = root.get("data");
            else if (root.has("statuses") && root.get("statuses").isArray()) arr = root.get("statuses");
            else if (!root.isArray()) arr = mapper.createArrayNode();

            String q = Optional.ofNullable(keyword).orElse("").toLowerCase(Locale.ROOT).trim();
            boolean noFilter = q.isBlank() || "*".equals(q);

            List<AnalyzedTweet> out = new ArrayList<>();
            for (JsonNode n : arr) {
                String text = n.path("text").asText(n.path("full_text").asText(n.path("content").asText("")));
                if (!noFilter && !text.toLowerCase(Locale.ROOT).contains(q)) continue;

                AnalyzedTweet t = new AnalyzedTweet();
                t.id = n.path("id_str").asText(n.path("id").asText(UUID.randomUUID().toString()));
                t.text = text;
                t.authorId = n.path("author_id").asText(n.path("user").path("id_str").asText(""));
                String created = n.path("created_at").asText("");
                try { t.createdAt = created.isBlank() ? OffsetDateTime.now() : OffsetDateTime.parse(created); }
                catch (Exception ex) { t.createdAt = OffsetDateTime.now(); }

                t.sentiment = CategoryClassifier.sentiment(text);
                t.adultFlag = CategoryClassifier.isAdult(text);
                t.faculties = CategoryClassifier.faculties(text);
                t.categories = CategoryClassifier.categories(text);

                out.add(t);
            }
            System.out.println("[mock] loaded items: " + out.size());
            return out;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private InputStream resolveMock() {
        try {
            var cpr = new ClassPathResource("mock_tweets.json");
            if (cpr.exists()) return cpr.getInputStream();
        } catch (Exception ignore) {}
        try {
            var p = java.nio.file.Paths.get("src","main","resources","mock_tweets.json");
            if (java.nio.file.Files.exists(p)) return java.nio.file.Files.newInputStream(p);
        } catch (Exception ignore) {}
        return null;
    }
}
