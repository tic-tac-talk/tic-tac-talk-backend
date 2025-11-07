package com.khi.voiceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class ClovaCallbackService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 전사 Json를 문자열로 처리 후 반환
    public String processClovaResult(String jsonResult) throws Exception {

        log.info("[Callback Raw JSON] {}", jsonResult);

        JsonNode root = objectMapper.readTree(jsonResult);
        JsonNode segments = root.path("segments");

        StringBuilder transcript = new StringBuilder();
        for (JsonNode seg : segments) {
            String speaker = seg.path("speaker").asText("unknown");
            String text = seg.path("text").asText("");
            transcript.append(String.format("%s: %s\n", speaker, text));
        }

        String formatted = transcript.toString().trim();
        log.info("전사 완료: " + formatted);

        return formatted;
    }
}
