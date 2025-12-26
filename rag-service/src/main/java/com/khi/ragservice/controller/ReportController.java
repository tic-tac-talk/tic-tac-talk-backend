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

@Tag(name = "Report API", description = """
        대화 분석 결과 보고서 조회 컨트롤러
        
        ## WebSocket 엔드포인트
        - **연결**: /api/v1/rag/ws-report
        - **프로토콜**: STOMP over WebSocket
        - **인증**: JWT 필요 (Authorization 헤더에 Bearer 토큰)
        
        ## 구독 토픽 및 수신 이벤트
        
        ### /user/queue/notify
        사용자 분석 요청 처리 완료 알림
        
        #### REPORT_COMPLETED
        ```json
        {
            "type": "REPORT_COMPLETED",
            "reportId": 3
        }
        ```
        """
        )
@Slf4j
@RestController
@RequestMapping("/rag")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @Operation(summary = "사용자별 보고서 제목 목록 조회", description = "로그인한 사용자의 모든 보고서 제목 목록을 페이지네이션으로 조회. 게이트웨이의 JWT 필터에서 전달된 X-User-Id 헤더를 사용. 상세 내용은 /report/{id}로 개별 조회.")
    @GetMapping("/reports")
    public ApiResponse<Page<ReportTitleDto>> getReportTitles(
            @RequestHeader("X-User-Id") String userId,
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

    @Operation(summary = "보고서 내의 이름 선택", description = "현 메서드는 입력 방식을 음성으로 했을 때에만 사용하는 메서드. Voice-Service에서 가상의 이름 A, B로 생성해놓고, 최종 리포트에서 로그인한 사용자가 먼저 A, B 중 어떤 대화가 본인 것인지 고름. 이후 본인의 이름과 다른 사람의 이름을 입력.")
    @PatchMapping("/report/{id}/user-name")
    public ApiResponse<ReportSummaryDto> updateUserName(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody UpdateUserNameRequestDto requestDto) {

        log.info("[ReportController] 화자 이름 설정 요청 - reportId: {}, userId: {}, selectedSpeaker: {}",
                id, userId, requestDto.getSelectedSpeaker());
        ReportSummaryDto updatedReport = reportService.updateUserName(id, userId, requestDto);
        return ApiResponse.success(updatedReport);
    }
}
