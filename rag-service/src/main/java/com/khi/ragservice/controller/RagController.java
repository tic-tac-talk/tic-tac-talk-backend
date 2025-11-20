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
public class RagController {

    private final RagService ragService;

    @PostMapping("/feign/receive")
    public RagResponseDto rag(@RequestBody RagRequestDto requestDto) {

        log.info("[RagContorller] 응답 수신 userId: {}", requestDto.getUserId());
        return ragService.getRagResponse(requestDto.getUserId(), requestDto.getChatData());
    }

    @GetMapping("/report/{id}")
    public RagResponseEntity getById(@PathVariable Long id) {

        log.info("[RagController] 조회 요청 id: {}", id);
        return ragService.getRagResponseById(id);
    }
}
