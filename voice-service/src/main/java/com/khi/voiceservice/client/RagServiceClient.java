package com.khi.voiceservice.client;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;


@Slf4j
@Component
public class RagServiceClient {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${spring.rag-service.receive-url}")
    private String ragServiceUrl;

    public String passScriptToRagService(String body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);

        HttpEntity<String> request = new HttpEntity<>(body, headers);

        ResponseEntity<String> response = restTemplate.postForEntity(ragServiceUrl, request, String.class);

        return response.getBody();
    }
}
