package com.khi.voiceservice.common.dto;

import lombok.Builder;

@Builder
public record UserInfo(
        String userId,
        String username,
        String nickname,
        String userRole,
        String profileUrl
) {
    public static String getUserId(UserInfo userInfo) {
        return userInfo.userId;
    }
    public static String getNickname(UserInfo userInfo) {
        return userInfo.nickname;
    }

}