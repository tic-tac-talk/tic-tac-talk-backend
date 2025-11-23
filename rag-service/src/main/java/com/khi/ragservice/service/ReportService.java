package com.khi.ragservice.service;

import com.khi.ragservice.common.exception.ResourceNotFoundException;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.ReportTitleDto;
import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.repository.ConversationReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportService {

        private final ConversationReportRepository conversationReportRepository;

        public ReportSummaryDto getReportById(Long id) {
                ConversationReport entity = conversationReportRepository.findById(id)
                                .orElseThrow(() -> new ResourceNotFoundException("ConversationReport", "id", id));
                return new ReportSummaryDto(
                                entity.getId(),
                                entity.getUser1Id(),
                                entity.getUser2Id(),
                                entity.getTitle(),
                                entity.getChatData(),
                                entity.getReportCards(),
                                entity.getCreatedAt());
        }

        public Page<ReportTitleDto> getUserReportTitles(String userId, Pageable pageable) {
                log.info("[ReportService] Retrieving report titles for userId: {} with pagination", userId);
                Page<ConversationReport> page = conversationReportRepository.findByUser1IdOrUser2Id(userId, userId,
                                pageable);

                return page.map(entity -> new ReportTitleDto(
                                entity.getId(),
                                entity.getTitle(),
                                entity.getCreatedAt()));
        }
}
