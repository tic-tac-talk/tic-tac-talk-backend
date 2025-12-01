package com.khi.ragservice.controller;

import com.khi.ragservice.dto.ChatRagRequestDto;
import com.khi.ragservice.dto.InitializeReportRequestDto;
import com.khi.ragservice.dto.RagRequestDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RAG API", description = "RAG 관련 Feign 요청 처리 전문 컨트롤러 (프론트 사용 X)")
@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @Operation(summary = "빈 보고서 초기화 (Voice-Service 전용)", description = "클라이언트로부터 대화 분석 요청을 받자마자 호출받아 PENDING 상태의 빈 보고서를 생성. 사용자가 즉시 '생성 중' 상태를 볼 수 있도록 함.")
    @PostMapping("/feign/initialize")
    public void initializeReport(@RequestBody InitializeReportRequestDto requestDto) {

        log.info("[RagController] 빈 보고서 초기화 요청 user1Id: {}, user1Name: {}, user2Id: {}, user2Name: {}",
                requestDto.getUser1Id(), requestDto.getUser1Name(),
                requestDto.getUser2Id(), requestDto.getUser2Name());
        ragService.initializeReport(
                requestDto.getUser1Id(), requestDto.getUser1Name(),
                requestDto.getUser2Id(), requestDto.getUser2Name());
    }

    @Operation(summary = "음성 텍스트를 수신하여 RAG 응답 생성 (Voice-Service 전용)", description = "Voice-Service, Chat-Service 등 다른 모듈에서 전달된 대화 텍스트를 바탕으로 감정 분석을 수행하고 RAG 응답을 반환.")
    @PostMapping("/feign/receive")
    public ReportSummaryDto analyzeChatConversationWithVoice(@RequestBody RagRequestDto requestDto) {

        log.info("[RagController] 응답 수신 user1Id: {}, user2Id: {}", requestDto.getUser1Id(), requestDto.getUser2Id());
        return ragService.analyzeConversation(requestDto.getUser1Id(), requestDto.getUser2Id(),
                requestDto.getChatData());
    }

    @Operation(summary = "채팅 대화 분석 (Chat-Service 전용)", description = "Chat-Service 전용. reportId를 직접 지정하여 대화 분석 수행 및 보고서 생성.")
    @PostMapping("/feign/chat/analyze")
    public ReportSummaryDto analyzeChatConversationWithChat(@RequestBody ChatRagRequestDto requestDto) {

        log.info("[RagController] 채팅 분석 요청 - reportId: {}, user1Id: {}, user2Id: {}",
                requestDto.getReportId(), requestDto.getUser1Id(), requestDto.getUser2Id());
        return ragService.analyzeConversationWithReportId(requestDto);
    }
}