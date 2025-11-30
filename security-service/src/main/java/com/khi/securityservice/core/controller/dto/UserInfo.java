package com.khi.securityservice.core.controller.dto;

import lombok.Builder;

@Builder
public record UserInfo(
        String userId,
        String nickname,
        String userRole,
        String profileUrl
) {
}
