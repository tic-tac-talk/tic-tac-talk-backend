package com.khi.voiceservice.controller;

import com.khi.voiceservice.client.ClovaSpeechClient;
import com.khi.voiceservice.client.RagServiceClient;
import com.khi.voiceservice.dto.RagRequestDto;
import com.khi.voiceservice.dto.RagResponseDto;
import com.khi.voiceservice.service.ClovaCallbackService;
import com.khi.voiceservice.service.NcpStorageService;
import com.khi.voiceservice.common.annotation.CurrentUser;
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
    private final ClovaCallbackService clovaCallbackService;
    private final RagServiceClient ragServiceClient;

    @Value("${clova.speech.callback-url}")
    private String callbackUrl;

    @PostMapping("/transcribe")
    public ResponseEntity<Void> transcribe(
            @RequestPart("file")MultipartFile voiceFile
    ) {
        String fileUrl = ncpStorageService.uploadFile(voiceFile);

        log.info("[Object Storage] fileUrl: " + fileUrl);

        clovaSpeechClient.asyncRecognize(fileUrl, callbackUrl);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> clovaCallback(
            @CurrentUser String userId,
            @RequestBody String resultJson
    ) {
        RagRequestDto body;
        try {
            body = clovaCallbackService.processClovaResult(userId, resultJson);

        } catch (IllegalArgumentException e) {
            log.warn("[Clova] 콜백 파싱 실패: {}", e.getMessage());

            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("[Clova] clova 콜백 처리 중 예외 발생", e);

            return ResponseEntity.internalServerError().build();
        }

        RagResponseDto response = ragServiceClient.passScriptToRagService(body);

        //TODO: 최종 결과물(response) 저장

        return ResponseEntity.ok().build();
    }
}
