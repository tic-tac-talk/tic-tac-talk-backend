package com.khi.chatservice.presentation.dto.res;

import com.jamjam.chat.domain.entity.ChatMessageEntity;

import java.time.LocalDateTime;

public record ChatSocketRes(
        Long messageId,
        String senderId,
        String senderNickname,
        String content,
        LocalDateTime sentAt
) {
    public static ChatSocketRes from(ChatMessageEntity entity, String senderNickname) {
        return new ChatSocketRes(
                entity.getId(),
                entity.getSenderId(),
                senderNickname,
                entity.getContent(),
                entity.getSentAt()
        );
    }
}