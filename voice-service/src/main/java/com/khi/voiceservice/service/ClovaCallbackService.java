package com.khi.voiceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.voiceservice.dto.ChatMessageDto;
import com.khi.voiceservice.dto.RagRequestDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class ClovaCallbackService {

    private final ObjectMapper objectMapper = new ObjectMapper();

    // 전사 Json를 문자열로 처리 후 반환
    public RagRequestDto processClovaResult(String userId, String jsonResult) throws Exception {

        log.info("[Callback Raw JSON] {}", jsonResult);

        JsonNode root = objectMapper.readTree(jsonResult);
        JsonNode segments = root.path("segments");

        List<ChatMessageDto> chatList = new ArrayList<>();
        for (JsonNode seg : segments) {
            String speaker = seg.path("speaker").path("name").asText("unknown");
            String text = seg.path("text").asText("");

            ChatMessageDto dto = new ChatMessageDto();
            dto.setName(speaker);
            dto.setMessage(text);

            chatList.add(dto);
        }

        RagRequestDto requestDto = new RagRequestDto();
        // TODO: User 생성 시 임의값 제거
        requestDto.setUserId(1L);
        requestDto.setChatData(chatList);

        log.info("[Clova Result] {}", requestDto);

        return requestDto;
    }
}
