package com.khi.chatservice.presentation.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReportCallbackRequestDto {
    private String type;
    private Long reportId;
    private Set<String> targetUserIds;
}
