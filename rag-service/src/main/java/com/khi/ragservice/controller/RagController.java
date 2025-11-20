package com.khi.ragservice.controller;

import com.khi.ragservice.dto.RagRequestDto;
import com.khi.ragservice.dto.RagResponseDto;
import com.khi.ragservice.entity.RagResponseEntity;
import com.khi.ragservice.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
@Tag(name = "RAG API", description = "RAG 관련 요청 및 결과 보고서 처리 컨트롤러")
public class RagController {

    private final RagService ragService;

    @Operation(
            summary = "다른 모듈로부터 음성 텍스트를 수신하여 분석",
            description = "Voice-Service, Chat-Service 등 다른 모듈에서 전달된 음성 인식 텍스트를 바탕으로 감정 분석을 수행하고 RAG 응답을 반환."
    )
    @PostMapping("/feign/receive")
    public RagResponseDto rag(@RequestBody RagRequestDto requestDto) {

        log.info("[RagContorller] 응답 수신 userId: {}", requestDto.getUserId());
        return ragService.getRagResponse(requestDto.getUserId(), requestDto.getChatData());
    }

    @Operation(
            summary = "감정 평가 결과 보고서 조회",
            description = "저장된 감정 평가 결과 보고서를 보고서 ID로 단건 조회."
    )
    @GetMapping("/report/{id}")
    public RagResponseEntity getById(@PathVariable Long id) {

        log.info("[RagController] 조회 요청 id: {}", id);
        return ragService.getRagResponseById(id);
    }
}