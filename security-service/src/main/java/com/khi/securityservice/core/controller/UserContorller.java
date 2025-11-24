package com.khi.securityservice.core.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.khi.securityservice.common.api.ApiResponse;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "User API", description = "사용자 인증 및 권한 관련 API")
@RestController
@RequestMapping("/security")
public class UserContorller {

    @Operation(summary = "로그아웃", description = "-")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logut() {

        return ResponseEntity.ok(ApiResponse.success());
    }
}
