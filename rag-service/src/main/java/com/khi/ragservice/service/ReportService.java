package com.khi.ragservice.service;

import com.khi.ragservice.common.exception.ResourceNotFoundException;
import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.ReportTitleDto;
import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.repository.ConversationReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
                                entity.getCreatedAt(),
                                entity.getState());
        }

        public Page<ReportTitleDto> getUserReportTitles(String userId, Pageable pageable) {
                log.info("[ReportService] Retrieving report titles for userId: {} with pagination", userId);
                Page<ConversationReport> page = conversationReportRepository.findByUser1IdOrUser2Id(userId, userId,
                                pageable);

                return page.map(entity -> new ReportTitleDto(
                                entity.getId(),
                                entity.getTitle(),
                                entity.getCreatedAt(),
                                entity.getState()));
        }

        @Transactional
        public ReportSummaryDto updateUserName(Long reportId, String newName) {
                log.info("[ReportService] Updating user2 name for reportId: {}, newName: {}",
                                reportId, newName);

                ConversationReport entity = conversationReportRepository.findById(reportId)
                                .orElseThrow(() -> new ResourceNotFoundException("ConversationReport", "id", reportId));

                String user2Id = entity.getUser2Id();

                // chatData에서 user2Id의 모든 메시지 name 업데이트
                if (entity.getChatData() != null) {
                        for (ChatMessageDto message : entity.getChatData()) {
                                if (user2Id.equals(message.getUserId())) {
                                        message.setName(newName);
                                }
                        }
                }

                ConversationReport savedEntity = conversationReportRepository.save(entity);
                log.info("[ReportService] Successfully updated user2 name in reportId: {}", reportId);

                return new ReportSummaryDto(
                                savedEntity.getId(),
                                savedEntity.getUser1Id(),
                                savedEntity.getUser2Id(),
                                savedEntity.getTitle(),
                                savedEntity.getChatData(),
                                savedEntity.getReportCards(),
                                savedEntity.getCreatedAt(),
                                savedEntity.getState());
        }
}
