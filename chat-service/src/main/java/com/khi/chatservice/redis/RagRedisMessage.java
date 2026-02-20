package com.khi.chatservice.redis;

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
public class RagRedisMessage implements Serializable {
    private String type;
    private Long reportId;
    private Set<String> targetUserIds;
}
