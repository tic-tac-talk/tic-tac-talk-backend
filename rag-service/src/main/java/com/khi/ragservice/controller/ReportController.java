package com.khi.ragservice.controller;

import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.UserReportsDto;
import com.khi.ragservice.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report API", description = "대화 분석 결과 보고서 조회 컨트롤러")
@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "대화 분석 결과 보고서 조회", description = "저장된 대화 분석 결과 보고서를 보고서 id로 단건 조회.")
    @GetMapping("/report/{id}")
    public ReportSummaryDto getReportById(@PathVariable Long id) {

        log.info("[ReportController] 조회 요청 id: {}", id);
        return reportService.getReportById(id);
    }

    @Operation(summary = "사용자별 모든 보고서 조회", description = "특정 사용자 ID에 해당하는 모든 대화 분석 결과 보고서를 조회. 유저 A, B 둘 다 본인 userId로 조회 가능.")
    @GetMapping("/reports/user/{userId}")
    public UserReportsDto getAllReportsByUserId(@PathVariable String userId) {

        log.info("[ReportController] 사용자별 조회 요청 userId: {}", userId);
        return reportService.getUserReports(userId);
    }
}
