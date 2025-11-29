package com.khi.ragservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 보고서 내 화자별 이름 설정 요청 DTO
 * voice-service에서 A, B로 구분된 화자 중 로그인 유저가 본인의 화자를 선택하고,
 * 각 화자에 실제 이름을 설정하기 위한 요청 데이터
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserNameRequestDto {
    private String selectedSpeaker; // "A" 또는 "B" - 로그인 유저가 선택한 화자
    private String loggedInUserName; // 로그인 유저의 실제 이름
    private String otherUserName; // 상대방 이름
}
