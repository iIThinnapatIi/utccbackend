package com.example.backend1.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.web.client.RestClient;

/**
 * รวมการตั้งค่า RestClient ทุกตัวไว้ที่เดียว
 * - typhoonRestClient: สำหรับเรียก API วิเคราะห์ (LLM / Typhoon backend)
 * - twitterRestClient: สำหรับเรียก Twitter API v2
 */
@Configuration
public class HttpClientConfig {

    /**
     * ✅ RestClient สำหรับเรียก Typhoon / LLM API
     * ใช้ค่าจาก application.properties:
     *
     * typhoon.base-url=https://your-typhoon-api
     * typhoon.api-key=YOUR_API_KEY
     */
    @Bean
    public RestClient typhoonRestClient(
            RestClient.Builder builder,
            @Value("${typhoon.base-url}") String baseUrl,
            @Value("${typhoon.api-key:}") String apiKey
    ) {
        RestClient.Builder b = builder
                .baseUrl(baseUrl)
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json");

        if (apiKey != null && !apiKey.isBlank()) {
            b.defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey);
        }

        return b.build();
    }

    /**
     * ✅ RestClient สำหรับเรียก Twitter API v2
     * ใช้ใน ingest/twitter เพื่อดึงข้อมูล tweet
     */
    @Bean
    public RestClient twitterRestClient(RestClient.Builder builder) {
        return builder
                .baseUrl("https://api.twitter.com/2")
                .defaultHeader(HttpHeaders.CONTENT_TYPE, "application/json")
                .build();
    }
}
