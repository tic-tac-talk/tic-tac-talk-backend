package com.khi.chatservice.presentation.dto.res;

import com.khi.chatservice.domain.entity.ChatMessageEntity;
import com.khi.chatservice.presentation.dto.res.SliceInfo;

import java.util.List;
import java.util.Map;

public record ChatHistoryRes(
        SliceInfo sliceInfo,
        List<ChatMessageRes> chats
) {
    public static ChatHistoryRes of(List<ChatMessageEntity> entities, SliceInfo sliceInfo, String currentUserId, Map<String, String> userIdToNickname, Map<String, String> userIdToProfileUrl) {
        List<ChatMessageRes> dtos = entities.stream()
                .map(e -> ChatMessageRes.from(
                        e,
                        currentUserId,
                        userIdToNickname.get(e.getSenderId()),
                        userIdToProfileUrl.get(e.getSenderId())
                ))
                .toList();
        return new ChatHistoryRes(sliceInfo, dtos);
    }
}
