package com.khi.voiceservice.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.voiceservice.Entity.Transcript;
import com.khi.voiceservice.dto.*;
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

    public Long getTranscriptId(UserPairRequest userPairRequest) {
        Transcript transcript = new Transcript();
        transcript.setUser1Id(userPairRequest.getUser1Id());
        transcript.setUser2Id(userPairRequest.getUser2Id());
        transcriptRepository.save(transcript);

        return transcript.getId();
    }
    // 전사 결과를 transcript entity에 저장하고 transcript를 반환
    public Transcript processClovaResult(String jsonResult) throws Exception {
        log.info("[Callback Raw JSON] {}", jsonResult);

        JsonNode root = objectMapper.readTree(jsonResult);

        JsonNode userdataNode = root.path("params").path("userdata");
        JsonNode segments = root.path("segments");

        // 요청 객체 확인, 이미 Rag 분석 요청된 객체라면 건너뜀
        Long transcriptId = userdataNode.path("transcriptId").asLong();
        Transcript transcript = transcriptRepository.findById(transcriptId)
                .orElseThrow(() -> new RuntimeException("Transcript Not found"));
        if (transcript.isClovaProcessed()) {
            log.info("[Transcript] 이미 rag 분석 요청된 객체입니다. 갱신 및 분석 요청을 건너뜁니다.");
            return null;
        }

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

        transcript.setChatData(chatList);
        transcript.setClovaProcessed(true);
        transcriptRepository.save(transcript);

        return transcript;
    }

    // RagRequestDto로 변환
    public RagRequestDto getRagRequestDto(Transcript transcript) {
        RagRequestDto ragRequestDto = new RagRequestDto();
        ragRequestDto.setUser1Id(transcript.getUser1Id());
        ragRequestDto.setUser2Id(transcript.getUser2Id());
        ragRequestDto.setChatData(transcript.getChatData());

        return ragRequestDto;
    }

    // Transcript와 RagReport 매칭
    public void matchTranscriptAndReport(Transcript transcript, ReportSummaryDto reportSummaryDto) {
        transcript.setConversationReportId(reportSummaryDto.getId());
        transcriptRepository.save(transcript);
    }
}
