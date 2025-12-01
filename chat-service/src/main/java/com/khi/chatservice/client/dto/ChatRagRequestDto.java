package com.khi.chatservice.client.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatRagRequestDto {
    private Long reportId;
    private String user1Id;
    private String user2Id;
    private List<ChatMessageDto> chatData;
}