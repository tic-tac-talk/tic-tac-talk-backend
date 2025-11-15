package com.khi.chatservice.presentation.dto.res;

import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.domain.entity.ChatMessageEntity;

import java.time.LocalDateTime;

public record ChatMessageRes(
        Long messageId,
        String senderId,
        String senderNickname,
        String content,
        LocalDateTime sentAt,
        Boolean isOwn
) {
    public static ChatMessageRes from(ChatMessageEntity entity, String currentUserId, String senderNickname) {
        return new ChatMessageRes(
                entity.getId(),
                entity.getSenderId(),
                senderNickname,
                entity.getContent(),
                entity.getSentAt(),
                entity.getSenderId().equals(currentUserId)
        );
    }

    public static ChatMessageRes of(ChatMessageEntity entity, String senderNickname) {
        return new ChatMessageRes(
                entity.getId(),
                entity.getSenderId(),
                senderNickname,
                entity.getContent(),
                entity.getSentAt(),
                null
        );
    }
}