package com.example.backend1.sentiment;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class SentimentClientConfig {

    @Bean(name = "sentimentRestTemplate")
    public RestTemplate sentimentRestTemplate(
            RestTemplateBuilder builder,
            @Value("${sentiment.timeout.seconds:5}") long timeoutSec
    ) {
        return builder
                .setConnectTimeout(Duration.ofSeconds(timeoutSec))
                .setReadTimeout(Duration.ofSeconds(timeoutSec))
                .build();
    }
}
