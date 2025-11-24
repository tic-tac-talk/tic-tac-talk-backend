package com.khi.ragservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class ChatMessageDto {
    private String userId;
    private String name;
    private String message;
}
