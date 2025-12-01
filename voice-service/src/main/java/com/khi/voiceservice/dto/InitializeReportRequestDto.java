package com.khi.voiceservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 빈 보고서 초기화 요청 DTO
 * voice-service가 음성 파일을 받자마자 호출하여 PENDING 상태의 보고서를 생성
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class InitializeReportRequestDto {
    private String user1Id;
    private String user2Id;
}
