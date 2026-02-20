package com.khi.ragservice.dto;

import com.khi.ragservice.enums.SourceType;
import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportCompletedEvent {
    private final Long reportId;
    private final String requestUserId1;
    private final String requestUserId2;
    private final SourceType sourceType;
}
