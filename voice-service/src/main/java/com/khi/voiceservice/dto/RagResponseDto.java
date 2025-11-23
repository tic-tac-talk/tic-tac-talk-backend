package com.khi.voiceservice.dto;

import com.khi.voiceservice.dto.reportcard.ReportCardDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RagResponseDto {
    private List<ChatMessageDto> chatData;
    private List<ReportCardDto> reportCards;
}
