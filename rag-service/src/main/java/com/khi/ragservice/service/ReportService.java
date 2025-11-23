package com.khi.ragservice.service;

import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.UserReportsDto;
import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.repository.ConversationReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

    private final ConversationReportRepository conversationReportRepository;

    public ReportSummaryDto getReportById(Long id) {
        ConversationReport entity = conversationReportRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("ConversationReport not found with id: " + id));
        return new ReportSummaryDto(
                entity.getId(),
                entity.getUser1Id(),
                entity.getUser2Id(),
                entity.getChatData(),
                entity.getReportCards(),
                entity.getCreatedAt());
    }

    public List<ConversationReport> getAllConversationReportsByUserId(String userId) {
        log.info("[ReportService] Retrieving all reports for userId: {}", userId);
        return conversationReportRepository.findByUser1IdOrUser2Id(userId, userId);
    }

    public UserReportsDto getUserReports(String userId) {
        log.info("[ReportService] Retrieving all reports for userId: {}", userId);
        List<ConversationReport> entities = conversationReportRepository.findByUser1IdOrUser2Id(userId, userId);

        List<ReportSummaryDto> reports = entities.stream()
                .map(entity -> new ReportSummaryDto(
                        entity.getId(),
                        entity.getUser1Id(),
                        entity.getUser2Id(),
                        entity.getChatData(),
                        entity.getReportCards(),
                        entity.getCreatedAt()))
                .toList();

        return new UserReportsDto(userId, reports);
    }
}
