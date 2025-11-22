package com.khi.voiceservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RagRequestDto {
    private String userId;
    private List<ChatMessageDto> chatData;
}
