package com.khi.voiceservice.controller;

import com.khi.voiceservice.Entity.Transcript;
import com.khi.voiceservice.client.ClovaSpeechClient;
import com.khi.voiceservice.client.RagClient;
import com.khi.voiceservice.common.api.ApiResponse;
import com.khi.voiceservice.dto.*;
import com.khi.voiceservice.service.TranscriptService;
import com.khi.voiceservice.service.NcpStorageService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@RestController
@RequestMapping("/voice")
@RequiredArgsConstructor
public class VoiceController {

    private final NcpStorageService ncpStorageService;
    private final ClovaSpeechClient clovaSpeechClient;
    private final TranscriptService transcriptService;
    private final RagClient ragClient;

    @Value("${clova.speech.callback-url}")
    private String callbackUrl;

    @Operation(summary = "전사 요청 API", description = "전사 완료 시, 비동기적으로 Rag 분석 실시")
    @PostMapping("/transcribe")
    public ResponseEntity<ApiResponse<VoiceResponseDto>> transcribe(
            @RequestHeader("X-User-Id") String userId,
            @RequestPart("file")MultipartFile voiceFile
    ) {
        log.info("[VOICE-SERVICE] Received X-User-Id header: {} for POST /transcribe", userId);

        InitializeReportRequestDto initializeReportRequest = new InitializeReportRequestDto("A", "B");
        ragClient.initializeReport(initializeReportRequest);

        String fileUrl = ncpStorageService.uploadFile(voiceFile);

        Long transcriptId = transcriptService.getTranscriptId(userId);
        VoiceResponseDto voiceResponse = new VoiceResponseDto(transcriptId);

        clovaSpeechClient.asyncRecognize(fileUrl, callbackUrl, transcriptId);

        return ResponseEntity.ok(ApiResponse.success(voiceResponse));
    }

    // 전사 결과 전달 받는 콜백
    @Operation(summary = "전사 완료 시 CLOVA 콜백함수")
    @PostMapping("/callback")
    public ResponseEntity<Void> clovaCallback(
            @RequestBody String resultJson
    ) {
        Transcript transcript;
        try {
            transcript = transcriptService.processClovaResult(resultJson);
        } catch (IllegalArgumentException e) {
            log.warn("[Clova] 콜백 파싱 실패: {}", e.getMessage());

            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[Clova] clova 콜백 처리 중 예외 발생", e);

            return ResponseEntity.internalServerError().build();
        }

        // 이미 분석 요청이 된 경우
        if (transcript == null) return ResponseEntity.ok().build();

        // Rag 분석 요청
        RagRequestDto ragRequest = new RagRequestDto("A", "B", transcript.getChatData());
        ReportSummaryDto reportSummaryDto = ragClient.getRagResult(ragRequest);

        // RagReportId와 Transcript 엔티티 매칭
        transcriptService.matchTranscriptAndReport(transcript, reportSummaryDto);

        return ResponseEntity.ok().build();
    }
}
