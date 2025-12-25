package com.khi.ragservice.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class ReportCompletedEvent {
    private final Long reportId;
    private final String requestUserId1;
    private final String requestUserId2;
}
