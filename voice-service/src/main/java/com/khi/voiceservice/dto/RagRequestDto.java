package com.khi.voiceservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RagRequestDto {
    private String user1Id;
    private String user2Id;
    private List<ChatMessageDto> chatData;
}
