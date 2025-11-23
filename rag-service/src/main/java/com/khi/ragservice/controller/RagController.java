package com.khi.ragservice.controller;

import com.khi.ragservice.dto.RagRequestDto;
import com.khi.ragservice.entity.RagResponseEntity;
import com.khi.ragservice.service.RagService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "RAG API", description = "RAG 관련 요청 및 결과 보고서 처리 컨트롤러")
@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @Operation(summary = "음성 텍스트를 수신하여 RAG 응답 생성", description = "Voice-Service, Chat-Service 등 다른 모듈에서 전달된 음성 인식 텍스트를 바탕으로 감정 분석을 수행하고 RAG 응답을 반환.")
    @PostMapping("/feign/receive")
    public RagResponseEntity analyzeChatConversation(@RequestBody RagRequestDto requestDto) {

        log.info("[RagController] 응답 수신 userId: {}", requestDto.getUserId());
        return ragService.getRagResponse(requestDto.getUserId(), requestDto.getChatData());
    }

    @Operation(summary = "감정 평가 결과 보고서 조회", description = "저장된 감정 평가 결과 보고서를 보고서 ID로 단건 조회.")
    @GetMapping("/report/{id}")
    public RagResponseEntity getById(@PathVariable Long id) {

        log.info("[RagController] 조회 요청 id: {}", id);
        return ragService.getRagResponseById(id);
    }
}