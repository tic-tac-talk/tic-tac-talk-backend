package com.khi.ragservice.controller;

import com.khi.ragservice.common.api.ApiResponse;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.ReportTitleDto;
import com.khi.ragservice.dto.UpdateUserNameRequestDto;
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

    @Operation(summary = "대화 분석 결과 보고서 단건 조회", description = "저장된 대화 분석 결과 보고서를 보고서 id로 단건 조회.")
    @GetMapping("/report/{id}")
    public ApiResponse<ReportSummaryDto> getReportById(@PathVariable Long id) {

        log.info("[ReportController] 조회 요청 id: {}", id);
        ReportSummaryDto report = reportService.getReportById(id);
        return ApiResponse.success(report);
    }

    @Operation(summary = "보고서 내 user2Id의 메시지 name 변경", description = "보고서의 chatData에서 user2Id에 해당하는 사용자의 모든 메시지 name을 변경. 음성으로 입력을 넣었을 때 user1Id는 로그인 되어 있으므로 user2Id의 name만 수정하면 됨.")
    @PatchMapping("/report/{id}/user-name")
    public ApiResponse<ReportSummaryDto> updateUserName(
            @PathVariable Long id,
            @RequestBody UpdateUserNameRequestDto requestDto) {

        log.info("[ReportController] user2 이름 변경 요청 reportId: {}, newName: {}",
                id, requestDto.getNewName());
        ReportSummaryDto updatedReport = reportService.updateUserName(id, requestDto.getNewName());
        return ApiResponse.success(updatedReport);
    }
}
