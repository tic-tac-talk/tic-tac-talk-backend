package com.khi.ragservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class ChatRagRequestDto {
    private Long reportId;
    private String user1Id;
    private String user2Id;
    private List<ChatMessageDto> chatData;
}