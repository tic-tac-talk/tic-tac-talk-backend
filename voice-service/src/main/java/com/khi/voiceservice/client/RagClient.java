package com.khi.voiceservice.client;

import com.khi.voiceservice.dto.RagRequestDto;
import com.khi.voiceservice.dto.RagResponseDto;
import com.khi.voiceservice.dto.ReportSummaryDto;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "rag-service", url = "${rag-service.url}")
public interface RagClient {

    @PostMapping("/rag/feign/receive")
    ReportSummaryDto getRagResult(@RequestBody RagRequestDto requestDto);
}
