package com.example.backend1.external.sentiment;

import com.example.backend1.external.sentiment.dto.AnalyzeRequest;
import com.example.backend1.external.sentiment.dto.AnalyzeResponse;
import com.example.backend1.external.sentiment.dto.BatchAnalyzeRequest;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.Arrays;
import java.util.List;

@Service
public class SentimentClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public SentimentClient(
            @Qualifier("sentimentRestTemplate") RestTemplate restTemplate,
            @Value("${sentiment.api.base-url}") String baseUrl
    ) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    public AnalyzeResponse analyze(String text) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<AnalyzeRequest> req = new HttpEntity<>(new AnalyzeRequest(text), headers);

            ResponseEntity<AnalyzeResponse> resp =
                    restTemplate.postForEntity(baseUrl + "/analyze", req, AnalyzeResponse.class);

            return resp.getBody() != null ? resp.getBody() : fallback("empty body");
        } catch (RestClientException e) {
            return fallback(e.getMessage());
        }
    }

    public List<AnalyzeResponse> analyzeBatch(List<String> texts) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            HttpEntity<BatchAnalyzeRequest> req = new HttpEntity<>(new BatchAnalyzeRequest(texts), headers);

            ResponseEntity<AnalyzeResponse[]> resp =
                    restTemplate.postForEntity(baseUrl + "/analyze_batch", req, AnalyzeResponse[].class);

            AnalyzeResponse[] body = resp.getBody();
            return body != null ? Arrays.asList(body) : List.of();
        } catch (RestClientException e) {
            return List.of(fallback(e.getMessage()));
        }
    }

    private AnalyzeResponse fallback(String reason) {
        AnalyzeResponse f = new AnalyzeResponse();
        f.setLabel("neu");
        f.setScore(0.0);
        f.setRaw("error:" + reason);
        return f;
    }
}
