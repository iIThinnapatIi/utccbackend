package com.example.backend1.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class HttpClientConfig {

    // RestClient สำหรับเรียก Twitter API v2
    @Bean
    public RestClient twitterRestClient(RestClient.Builder builder) {
        return builder.baseUrl("https://api.twitter.com/2").build();
    }
}
