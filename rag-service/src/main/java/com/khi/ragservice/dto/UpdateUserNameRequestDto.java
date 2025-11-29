package com.khi.ragservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 보고서 내 user2 이름 변경 요청 DTO
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateUserNameRequestDto {
    private String newName;
}
