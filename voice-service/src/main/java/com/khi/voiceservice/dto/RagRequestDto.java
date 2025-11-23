package com.khi.voiceservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
public class RagRequestDto {
    private Long userId;
    private List<ChatMessageDto> chatData;
}
