package com.khi.ragservice.controller;

import com.khi.ragservice.dto.RagRequestDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RAG API", description = "RAG 관련 Feign 요청 처리 전문 컨트롤러")
@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @Operation(summary = "음성 텍스트를 수신하여 RAG 응답 생성", description = "Voice-Service, Chat-Service 등 다른 모듈에서 전달된 음성 인식 텍스트를 바탕으로 감정 분석을 수행하고 RAG 응답을 반환.")
    @PostMapping("/feign/receive")
    public ReportSummaryDto analyzeChatConversation(@RequestBody RagRequestDto requestDto) {

        log.info("[RagController] 응답 수신 user1Id: {}, user2Id: {}", requestDto.getUser1Id(), requestDto.getUser2Id());
        return ragService.analyzeConversation(requestDto.getUser1Id(), requestDto.getUser2Id(),
                requestDto.getChatData());
    }
}