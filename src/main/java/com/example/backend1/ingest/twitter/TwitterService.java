package com.example.backend1.ingest.twitter;

import com.example.backend1.analysis.entity.TwAnalysis;
import com.example.backend1.analysis.repo.TwAnalysisRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TwitterService {

    private static final Logger log = LoggerFactory.getLogger(TwitterService.class);

    // ประกาศใน config/HttpClientConfig
    private final RestClient twitterRestClient;

    private final TweetRepository tweetRepository;          // เก็บข้อมูลดิบ
    private final TwAnalysisRepository twAnalysisRepository; // ตารางผลวิเคราะห์ (ของจริงที่ใช้)

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${twitter.bearer-token:}")
    private String bearerToken;

    public TwitterService(
            RestClient twitterRestClient,
            TweetRepository tweetRepository,
            TwAnalysisRepository twAnalysisRepository
    ) {
        this.twitterRestClient = twitterRestClient;
        this.tweetRepository = tweetRepository;
        this.twAnalysisRepository = twAnalysisRepository;
    }

    /** เรียก Twitter API v2 recent search */
    public String searchTweets(String keyword) {
        try {
            RestClient.RequestHeadersSpec<?> req = twitterRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tweets/search/recent")
                            .queryParam("query", keyword)
                            .queryParam("max_results", "29")
                            .queryParam("tweet.fields", "author_id,created_at")
                            .build());

            if (bearerToken != null && !bearerToken.isBlank()) {
                req = req.header(HttpHeaders.AUTHORIZATION, "Bearer " + bearerToken);
            } else {
                log.warn("twitter.bearer-token ว่าง: จะเรียกแบบไม่มี Authorization (อาจได้ 401)");
            }

            String json = req.retrieve().body(String.class);
            log.info("JSON from Twitter API: {}", json);
            return json != null ? json : "{}";
        } catch (Exception e) {
            log.error("searchTweets error", e);
            return "{}";
        }
    }

    /** แปลง JSON → Tweet (ข้อมูลดิบ) แล้วบันทึกลง DB */
    public void saveTweetsToDB(String jsonResponse) {
        try {
            JsonNode root = objectMapper.readTree(jsonResponse);
            JsonNode data = root.get("data");

            if (data != null && data.isArray()) {
                for (JsonNode node : data) {
                    Tweet tweet = new Tweet();
                    tweet.setId(node.get("id").asText());
                    tweet.setText(node.get("text").asText());
                    tweet.setAuthorId(node.get("author_id").asText());
                    tweet.setCreatedAt(node.get("created_at").asText());

                    log.debug("Saving tweet: {}", tweet.getText());
                    tweetRepository.save(tweet);
                }
            } else {
                log.info("No tweets found in JSON response.");
            }
        } catch (Exception e) {
            log.error("saveTweetsToDB error", e);
        }
    }

    /** ดึงจาก DB ดิบ พร้อมฟิลเตอร์ keyword แบบง่าย */
    public List<Tweet> getTweetsFromDB(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return tweetRepository.findAll();
        }
        return tweetRepository.findAll().stream()
                .filter(t -> t.getText() != null &&
                        t.getText().toLowerCase().contains(keyword.toLowerCase()))
                .toList();
    }

    /** โหลด mock_tweets.json จาก resources มาบันทึก (ใช้ทดสอบ dev) */
    public void saveMockTweets() {
        try (InputStream is = getClass().getResourceAsStream("/mock_tweets.json")) {
            if (is == null) {
                log.warn("mock_tweets.json not found in resources!");
                return;
            }
            String jsonResponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);
            saveTweetsToDB(jsonResponse);
        } catch (Exception e) {
            log.error("saveMockTweets error", e);
        }
    }

    /** ดึงผลวิเคราะห์ทั้งหมด (ตารางจริง TwAnalysis) */
    public List<TwAnalysis> getAllAnalysis() {
        return twAnalysisRepository.findAll();
    }
}
