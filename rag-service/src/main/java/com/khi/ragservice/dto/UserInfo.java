package com.khi.ragservice.dto;

import lombok.Builder;

@Builder
public record UserInfo(
        String userId,
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
    public static String getUserRole(UserInfo userInfo) { return userInfo.userRole; }
    public static String getProfileUrl(UserInfo userInfo) { return userInfo.profileUrl; }
}
