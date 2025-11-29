package com.khi.securityservice.core.controller;

import com.khi.securityservice.core.controller.dto.UserProfileRequestDto;
import com.khi.securityservice.core.controller.dto.UserProfileResponseDto;
import com.khi.securityservice.core.service.UserService;
import org.springframework.web.bind.annotation.*;

import com.khi.securityservice.common.api.ApiResponse;

import org.springframework.http.ResponseEntity;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.multipart.MultipartFile;

@Tag(name = "User API", description = "사용자 인증 및 권한 관련 API")
@RestController
@RequestMapping("/security")
public class UserContorller {

    private final UserService userService;

    public UserContorller(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "로그아웃", description = "-")
    @PostMapping("/logout")
    public ResponseEntity<ApiResponse<?>> logut() {

        return ResponseEntity.ok(ApiResponse.success());
    }

    @PutMapping("/user/additional-info")
    public ResponseEntity<UserProfileResponseDto> updateUserProfile(
            @RequestHeader("X-User-Id") String userId,
            @RequestParam(required = false) String nickname,
            @RequestParam(required = false) MultipartFile image,
            @RequestParam boolean isProfileImageDeleted
    ) {
        UserProfileResponseDto userProfileResponseDto =
                userService.updateUserProfile(userId, nickname, image, isProfileImageDeleted);

        return ResponseEntity.ok(userProfileResponseDto);
    }
}