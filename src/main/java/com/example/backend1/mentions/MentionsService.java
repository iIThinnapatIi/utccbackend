package com.example.backend1.mentions;

import com.example.backend1.Twitter.TweetRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class MentionsService {

    private final WebClient web;
    private final ObjectMapper om = new ObjectMapper();
    private final ClassificationService clf;

    @Value("${TW_BEARER:}")
    private String bearer;

    public MentionsService(ClassificationService clf, WebClient.Builder builder) {
        this.clf = clf;
        this.web = builder.baseUrl("https://api.twitter.com/2").build();
    }

    public SearchResponse search(String q, int max) {
        List<MentionDTO> items = fetchTwitter(q, max);
        // ทำ facet summary
        Map<String, Long> facetSent = items.stream().collect(Collectors.groupingBy(MentionDTO::sentiment, Collectors.counting()));
        Map<String, Long> facetCat  = items.stream().flatMap(i -> i.categories().stream())
                .collect(Collectors.groupingBy(s->s, Collectors.counting()));
        Map<String, Long> facetFac  = items.stream().flatMap(i ->
                        i.categories().stream().filter(c -> c.startsWith("คณะ") || c.equals("UTCC")))
                .collect(Collectors.groupingBy(s->s, Collectors.counting()));
        return new SearchResponse(items, facetSent, facetCat, facetFac);
    }

    private List<MentionDTO> fetchTwitter(String q, int max) {
        try {
            String json = tryTwitterAPI(q, max);
            if (json == null) json = readMock();
            JsonNode root = om.readTree(json);
            JsonNode data = root.get("data");
            if (data == null || !data.isArray()) return List.of();

            List<MentionDTO> list = new ArrayList<>();
            for (JsonNode n : data) {
                String id = n.path("id").asText();
                String text = n.path("text").asText("");
                String author = n.path("author_id").asText("");
                Instant ts = Instant.parse(n.path("created_at").asText("2025-01-01T00:00:00Z"));

                String sentiment = clf.detectSentiment(text);
                List<String> cats = clf.detectCategories(text);

                list.add(new MentionDTO(id, text, author, ts, cats, sentiment));
            }
            return list;
        } catch (Exception e) {
            return List.of(); // fail soft
        }
    }

    private String tryTwitterAPI(String q, int max) {
        if (bearer == null || bearer.isBlank()) return null;
        return web.get()
                .uri(uri -> uri.path("/tweets/search/recent")
                        .queryParam("query", q)
                        .queryParam("max_results", Math.min(Math.max(max,10),100))
                        .queryParam("tweet.fields", "author_id,created_at")
                        .build())
                .header("Authorization", "Bearer " + bearer)
                .retrieve()
                .onStatus(org.springframework.http.HttpStatusCode::isError, resp -> reactor.core.publisher.Mono.empty())
                .bodyToMono(String.class)
                .blockOptional().orElse(null);
    }

    private String readMock() throws Exception {
        // ใช้ไฟล์ mock ใน resources (มีอยู่แล้วในโปรเจกต์ของคุณ)【:contentReference[oaicite:3]{index=3}】
        var res = new ClassPathResource("mock_tweets.json");
        return new String(res.getInputStream().readAllBytes(), StandardCharsets.UTF_8);
    }
}
