package com.khi.voiceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.voiceservice.Entity.Transcript;
import com.khi.voiceservice.dto.ChatMessageDto;
import com.khi.voiceservice.dto.RagRequestDto;
import com.khi.voiceservice.repository.TranscriptRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranscriptService {

    private final TranscriptRepository transcriptRepository;

    private final ObjectMapper objectMapper = new ObjectMapper();

    public Long getTranscriptId() {
        Transcript transcript = new Transcript();
        transcriptRepository.save(transcript);

        return transcript.getId();
    }
    // 전사 Json를 문자열로 처리 후 반환, 전사 결과 저장
    public RagRequestDto processClovaResult(String jsonResult) throws Exception {

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

        log.info("[Transcript] " + chatList);

        JsonNode userdataNode = root.path("params").path("userdata");
        Long transcriptId = userdataNode.path("transcriptId").asLong();
        String user1Id = userdataNode.path("user1Id").asText();
        String user2Id = userdataNode.path("user2Id").asText();

        Transcript transcript = transcriptRepository.findById(transcriptId)
                .orElseThrow(() -> new RuntimeException("Transcript Not found"));

        if (transcript.isRagProcessed()) {
            log.info("[Transcript] 이미 rag 분석 요청된 객체입니다. 분석 요청을 건너뜁니다.");
            return null;
        }
        transcript.setUserId(user1Id);
        transcript.setChatData(chatList);
        transcript.setRagProcessed(true);
        transcriptRepository.save(transcript);

        RagRequestDto ragRequestDto = new RagRequestDto();
        ragRequestDto.setUser1Id(user1Id);
        ragRequestDto.setUser2Id(user2Id);
        ragRequestDto.setChatData(chatList);

        return ragRequestDto;
    }
}
