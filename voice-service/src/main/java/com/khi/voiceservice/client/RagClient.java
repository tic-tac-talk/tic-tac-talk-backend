package com.khi.voiceservice.client;

import com.khi.voiceservice.dto.RagRequestDto;
import com.khi.voiceservice.dto.RagResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rag-service")
public interface RagClient {

    @PostMapping("/rag/feign/receive")
    RagResponseDto getRagResult(@RequestBody RagRequestDto requestDto);
}
