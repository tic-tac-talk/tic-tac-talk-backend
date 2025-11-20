package com.khi.ragservice.controller;

import com.khi.ragservice.dto.RagRequestDto;
import com.khi.ragservice.dto.RagResponseDto;
import com.khi.ragservice.service.RagService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class RagController {

    private final RagService ragService;

    @PostMapping("/receive")
    public RagResponseDto rag(@RequestBody RagRequestDto requestDto) {

        log.info("[RagContorller] 응답 수신 userId: {}", requestDto.getUserId());
        return ragService.getRagResponse(requestDto.getChatData());
    }
}
