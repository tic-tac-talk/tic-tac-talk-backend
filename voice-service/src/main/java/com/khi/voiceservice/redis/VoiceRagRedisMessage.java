package com.khi.voiceservice.redis;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VoiceRagRedisMessage implements Serializable {
    private String type;
    private Long reportId;
    private Set<String> targetUserIds;
}
