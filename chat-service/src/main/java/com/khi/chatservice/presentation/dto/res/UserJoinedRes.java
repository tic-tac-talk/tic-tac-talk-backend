package com.khi.chatservice.presentation.dto.res;

import lombok.Builder;

import java.util.List;

@Builder
public record UserJoinedRes(
        String userId,
        String nickname,
        String message,
        List<ParticipantInfo> participants
) {
    @Builder
    public record ParticipantInfo(
            String userId,
            String nickname,
            String profileUrl
    ) {
    }
}