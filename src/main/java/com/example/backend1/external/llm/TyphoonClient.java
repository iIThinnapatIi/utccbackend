// src/main/java/com/example/backend1/external/llm/TyphoonClient.java
package com.example.backend1.external.llm;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

@Component
public class TyphoonClient {

    private final RestClient typhoonRestClient;

    // ✅ เขียน constructor เอง + ระบุ @Qualifier ให้ฉีด bean ที่ชื่อ typhoonRestClient
    public TyphoonClient(@Qualifier("typhoonRestClient") RestClient typhoonRestClient) {
        this.typhoonRestClient = typhoonRestClient;
    }

    public TyphoonResponse analyze(TyphoonRequest req) {
        return typhoonRestClient.post()
                .uri("/analyze") // ปรับ path ให้ตรง endpoint จริง
                .contentType(MediaType.APPLICATION_JSON)
                .body(req)
                .retrieve()
                .body(TyphoonResponse.class);
    }
}
