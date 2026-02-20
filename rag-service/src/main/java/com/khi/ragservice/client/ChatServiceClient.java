package com.khi.ragservice.client;

import com.khi.ragservice.dto.ReportCallbackDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "chat-service", url = "${chat-service.url}")
public interface ChatServiceClient {

    @PostMapping("/chat/feign/rag/callback")
    void sendReportCompletedCallback(@RequestBody ReportCallbackDto callbackDto);
}
