package com.khi.ragservice.controller;

import com.khi.ragservice.common.api.ApiResponse;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.ReportTitleDto;
import com.khi.ragservice.service.ReportService;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Report API", description = "대화 분석 결과 보고서 조회 컨트롤러")
@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "사용자별 보고서 제목 목록 조회", description = "특정 사용자 ID에 해당하는 모든 보고서의 제목 목록을 페이지네이션으로 조회. 상세 내용은 /report/{id}로 개별 조회.")
    @GetMapping("/reports/user/{userId}")
    public ApiResponse<Page<ReportTitleDto>> getReportTitlesByUserId(
            @PathVariable String userId,
            @PageableDefault(size = 10, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        log.info("[ReportController] 사용자별 보고서 제목 목록 조회 요청 userId: {}, page: {}, size: {}",
                userId, pageable.getPageNumber(), pageable.getPageSize());
        Page<ReportTitleDto> reports = reportService.getUserReportTitles(userId, pageable);
        return ApiResponse.success(reports);
    }

    @Operation(summary = "대화 분석 결과 보고서 조회", description = "저장된 대화 분석 결과 보고서를 보고서 id로 단건 조회.")
    @GetMapping("/report/{id}")
    public ApiResponse<ReportSummaryDto> getReportById(@PathVariable Long id) {

        log.info("[ReportController] 조회 요청 id: {}", id);
        ReportSummaryDto report = reportService.getReportById(id);
        return ApiResponse.success(report);
    }
}
