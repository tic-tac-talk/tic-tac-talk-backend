package com.khi.securityservice.core.controller.dto;

import lombok.Builder;

@Builder
public record UserInfo(
        String userId,
        String name,
        String nickname,
        String role
) {
}
