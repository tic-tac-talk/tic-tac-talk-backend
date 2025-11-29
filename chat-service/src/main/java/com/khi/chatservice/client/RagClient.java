package com.khi.chatservice.client;

import com.khi.chatservice.client.dto.RagRequestDto;
import io.swagger.v3.oas.annotations.Hidden;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@Hidden
@FeignClient(name = "rag-service", url = "${rag-service.url}")
public interface RagClient {

    @PostMapping("/rag/feign/receive")
    void analyzeConversation(@RequestBody RagRequestDto request);
}