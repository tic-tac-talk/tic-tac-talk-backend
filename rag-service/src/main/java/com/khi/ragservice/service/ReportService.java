package com.khi.ragservice.service;

import com.khi.ragservice.common.exception.ResourceNotFoundException;
import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.ReportTitleDto;
import com.khi.ragservice.dto.UpdateUserNameRequestDto;
import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.repository.ConversationReportRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

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
                                entity.getUser1Name(),
                                entity.getUser2Id(),
                                entity.getUser2Name(),
                                entity.getTitle(),
                                entity.getChatData(),
                                entity.getReportCards(),
                                entity.getCreatedAt(),
                                entity.getState(),
                                entity.getSourceType());
        }

        public Page<ReportTitleDto> getUserReportTitles(String userId, Pageable pageable) {
                log.info("[ReportService] Retrieving report titles for userId: {} with pagination", userId);
                Page<ConversationReport> page = conversationReportRepository.findByUser1IdOrUser2Id(userId, userId,
                                pageable);

                return page.map(entity -> new ReportTitleDto(
                                entity.getId(),
                                entity.getUser1Id(),
                                entity.getUser1Name(),
                                entity.getUser2Id(),
                                entity.getUser2Name(),
                                entity.getTitle(),
                                entity.getCreatedAt(),
                                entity.getState(),
                                entity.getSourceType()));
        }

        @Transactional
        public ReportSummaryDto updateUserName(Long reportId, String userId, UpdateUserNameRequestDto requestDto) {
                log.info("[ReportService] 화자 선택 기반 이름 업데이트 - reportId: {}, userId: {}, selectedSpeaker: {}",
                                reportId, userId, requestDto.getSelectedSpeaker());

                ConversationReport entity = conversationReportRepository.findById(reportId)
                                .orElseThrow(() -> new ResourceNotFoundException("ConversationReport", "id", reportId));

                List<ChatMessageDto> chatData = entity.getChatData();
                if (chatData == null || chatData.isEmpty()) {
                        log.warn("[ReportService] chatData가 비어있음 - reportId: {}", reportId);
                        return new ReportSummaryDto(
                                        entity.getId(),
                                        entity.getUser1Id(),
                                        entity.getUser1Name(),
                                        entity.getUser2Id(),
                                        entity.getUser2Name(),
                                        entity.getTitle(),
                                        entity.getChatData(),
                                        entity.getReportCards(),
                                        entity.getCreatedAt(),
                                        entity.getState(),
                                        entity.getSourceType());
                }

                // selectedSpeaker 검증
                String selectedSpeaker = requestDto.getSelectedSpeaker();
                if (!"A".equals(selectedSpeaker) && !"B".equals(selectedSpeaker)) {
                        throw new IllegalArgumentException(
                                        "selectedSpeaker must be 'A' or 'B', but got: " + selectedSpeaker);
                }

                // 화자 기반 이름 및 userId 업데이트
                // selectedSpeaker가 "A"면 user1Id를, "B"면 user2Id를 로그인 유저 ID로 업데이트
                if ("A".equals(selectedSpeaker)) {
                        entity.setUser1Id(userId);
                        log.info("[ReportService] user1Id 업데이트 - reportId: {}, user1Id: {}", reportId, userId);
                } else {
                        entity.setUser2Id(userId);
                        log.info("[ReportService] user2Id 업데이트 - reportId: {}, user2Id: {}", reportId, userId);
                }

                for (ChatMessageDto message : chatData) {
                        if (selectedSpeaker.equals(message.getName())) {
                                // 로그인 유저가 선택한 화자 → 실제 이름으로 변경
                                message.setName(requestDto.getLoggedInUserName());
                        } else {
                                // 나머지 화자 → 상대방 이름으로 변경
                                message.setName(requestDto.getOtherUserName());
                        }
                }

                ConversationReport savedEntity = conversationReportRepository.save(entity);
                log.info("[ReportService] 이름 업데이트 완료 - reportId: {}", reportId);

                return new ReportSummaryDto(
                                savedEntity.getId(),
                                savedEntity.getUser1Id(),
                                savedEntity.getUser1Name(),
                                savedEntity.getUser2Id(),
                                savedEntity.getUser2Name(),
                                savedEntity.getTitle(),
                                savedEntity.getChatData(),
                                savedEntity.getReportCards(),
                                savedEntity.getCreatedAt(),
                                savedEntity.getState(),
                                savedEntity.getSourceType());
        }
}
