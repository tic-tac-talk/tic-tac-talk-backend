package com.khi.voiceservice.client;

import com.khi.voiceservice.dto.RagRequestDto;
import com.khi.voiceservice.dto.RagResponseDto;
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

    public RagResponseDto passScriptToRagService(RagRequestDto body) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        HttpEntity<RagRequestDto> request = new HttpEntity<>(body, headers);

        ResponseEntity<RagResponseDto> response = restTemplate.postForEntity(ragServiceUrl, request, RagResponseDto.class);

        log.info("[Rag Result] " + response.getBody());

        return response.getBody();
    }
}
