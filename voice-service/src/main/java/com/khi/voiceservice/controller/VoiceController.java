package com.khi.voiceservice.controller;

import com.khi.voiceservice.Entity.Transcript;
import com.khi.voiceservice.client.ClovaSpeechClient;
import com.khi.voiceservice.client.RagClient;
import com.khi.voiceservice.dto.*;
import com.khi.voiceservice.service.TranscriptService;
import com.khi.voiceservice.service.NcpStorageService;
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

    @PostMapping("/transcribe")
    public ResponseEntity<VoiceResponseDto> transcribe(
            @RequestPart("userdata") UserPairRequest userPairRequest,
            @RequestPart("file")MultipartFile voiceFile
    ) {
        String fileUrl = ncpStorageService.uploadFile(voiceFile);

        Long transcriptId = transcriptService.getTranscriptId(userPairRequest);
        VoiceResponseDto voiceResponseDto = new VoiceResponseDto();
        voiceResponseDto.setTranscriptId(transcriptId);

        clovaSpeechClient.asyncRecognize(fileUrl, callbackUrl, transcriptId);

        return ResponseEntity.ok(voiceResponseDto);
    }

    // 전사 결과 전달 받는 콜백
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
        if (transcript == null) {
            return ResponseEntity.ok().build();
        }

        // Rag 분석 요청
        RagRequestDto ragRequestDto = transcriptService.getRagRequestDto(transcript);
        ReportSummaryDto reportSummaryDto = ragClient.getRagResult(ragRequestDto);
        log.info("[Rag] 분석 완료");

        // RagReportId와 Transcript 엔티티 매칭
        transcriptService.matchTranscriptAndReport(transcript, reportSummaryDto);

        return ResponseEntity.ok().build();
    }
}
