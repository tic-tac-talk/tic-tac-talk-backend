package com.khi.voiceservice.dto;

import com.khi.voiceservice.dto.reportcard.ReportCardDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportSummaryDto {
    private Long id;
    private String user1Id;
    private String user1Name;
    private String user2Id;
    private String user2Name;
    private String title;
    private List<ChatMessageDto> chatData;
    private List<ReportCardDto> reportCards;
    private LocalDateTime createdAt;
}
