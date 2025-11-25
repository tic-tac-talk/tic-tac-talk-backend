package com.khi.voiceservice.dto;

import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class UserPairRequest {
    String user1Id;
    String user2Id;
}
