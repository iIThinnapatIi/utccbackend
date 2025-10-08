package com.example.backend1.Twitter;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Service
public class TwitterService {

    private final WebClient webClient;
    private final TweetRepository tweetRepository;
    private final ObjectMapper objectMapper = new ObjectMapper();

    private static final String BEARER_TOKEN = "ถ้าจะดึงเพิ่ม ใส่api tw ของพวกมึงตรงนี้";

    public TwitterService(WebClient.Builder builder, TweetRepository tweetRepository) {
        this.webClient = builder.baseUrl("https://api.twitter.com/2").build();
        this.tweetRepository = tweetRepository;
    }

    public String searchTweets(String keyword) {
        try {
            String json = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/tweets/search/recent")
                            .queryParam("query", keyword)
                            .queryParam("max_results", "29")
                            .queryParam("tweet.fields", "author_id,created_at")
                            .build())
                    .header("Authorization", "Bearer " + BEARER_TOKEN)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            System.out.println("JSON from Twitter API: " + json); // debug
            return json;

        } catch (Exception e) {
            e.printStackTrace();
            return "{}";
        }
    }

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

                    System.out.println("Saving tweet: " + tweet.getText()); // debug
                    tweetRepository.save(tweet);
                }
            } else {
                System.out.println("No tweets found in JSON response.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<Tweet> getTweetsFromDB(String keyword) {
        if (keyword == null || keyword.isEmpty()) {
            return tweetRepository.findAll();
        } else {
            return tweetRepository.findAll().stream()
                    .filter(t -> t.getText().toLowerCase().contains(keyword.toLowerCase()))
                    .toList();
        }
    }

    //ทดสอบดึงฐานข้อมูลปลอมๆ อยู่ในไฟล์mock_tweets.json
    public void saveMockTweets() {
        try {
            // อ่าน JSON จากไฟล์
            InputStream is = getClass().getResourceAsStream("/mock_tweets.json");
            String jsonResponse = new String(is.readAllBytes(), StandardCharsets.UTF_8);

            // เรียก method เดิมของเรา
            saveTweetsToDB(jsonResponse);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}

