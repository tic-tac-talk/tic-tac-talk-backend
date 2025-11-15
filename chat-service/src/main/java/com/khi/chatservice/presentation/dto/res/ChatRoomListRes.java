package com.khi.chatservice.presentation.dto.res;

import lombok.Builder;

import java.time.LocalDateTime;
import java.util.List;

@Builder
public record ChatRoomListRes(
        List<ChatRoomSummary> rooms,
        Integer currentPage,
        Integer totalPages,
        Boolean hasNext
) {
    @Builder
    public record ChatRoomSummary(
            Long id,
            String nickname,
            String lastMessage,
            LocalDateTime lastMessageTime,
            Integer unreadCount,
            String profileUrl
    ) {
    }
}