package com.khi.voiceservice.controller;

import com.khi.voiceservice.csr.ClovaSpeechClient;
import com.khi.voiceservice.service.ClovaCallbackService;
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
    private final ClovaCallbackService clovaCallbackService;

    @Value("${clova.speech.callback-url}")
    private String callbackUrl;

    @PostMapping("/transcribe")
    public ResponseEntity<Void> transcribe(
            @RequestPart("file")MultipartFile voiceFile
    ) {
        String fileUrl = ncpStorageService.uploadFile(voiceFile);

        clovaSpeechClient.asyncRecognize(fileUrl, callbackUrl);

        return ResponseEntity.ok().build();
    }

    @PostMapping("/callback")
    public ResponseEntity<Void> clovaCallback(@RequestBody String resultJson) {
        log.info("클로바로부터 clovaCallback 호출");

        try {
            String transcript = clovaCallbackService.processClovaResult(resultJson);

            // TODO: gpt-service에 transcript 전달

            return ResponseEntity.ok().build();
        } catch (IllegalArgumentException e) {
            log.warn("콜백 파싱 실패: {}", e.getMessage());

            return ResponseEntity.badRequest().build();
        } catch (Exception e) {
            log.error("clova 콜백 처리 중 예외 발생", e);

            return ResponseEntity.internalServerError().build();
        }
    }
}
