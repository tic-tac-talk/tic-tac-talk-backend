package com.khi.ragservice.dto;

import com.khi.ragservice.enums.ReportState;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportTitleDto {
    private Long id;
    private String user1Id;
    private String user1Name;
    private String user2Id;
    private String user2Name;
    private String title;
    private LocalDateTime createdAt;
    private ReportState state;
}
