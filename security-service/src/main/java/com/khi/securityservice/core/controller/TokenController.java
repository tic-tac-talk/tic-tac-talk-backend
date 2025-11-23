package com.khi.securityservice.core.controller;

import com.khi.securityservice.common.api.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Token API", description = "인증 토큰 재발급 관련 API")
@RestController
@RequestMapping("/security/jwt")
public class TokenController {

    /* 실제 사용되지 않지만 Swagger에 표시하기 위해 등록 */
    @Operation(
            summary = "토큰 재발급",
            description = "Access, Refresh 토큰 전부 재발급"
    )
    @PostMapping("/reissue")
    public ResponseEntity<ApiResponse<?>> join() {

        return ResponseEntity.ok(ApiResponse.success());
    }
}

