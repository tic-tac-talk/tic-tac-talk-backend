package com.khi.voiceservice.client;

import com.khi.voiceservice.dto.RagRequestDto;
import com.khi.voiceservice.dto.RagResponseDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(
        name = "rag-service",
        url = "${spring.rag-service.base-url:http://rag-service:8080}"
)
public interface RagClient {

    @PostMapping("/rag/feign/receive")
    RagResponseDto getRagResult(@RequestBody RagRequestDto requestDto);
}
