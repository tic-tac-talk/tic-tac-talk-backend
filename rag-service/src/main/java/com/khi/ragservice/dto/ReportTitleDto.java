package com.khi.ragservice.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReportTitleDto {
    private Long id;
    private String title;
    private LocalDateTime createdAt;
}
