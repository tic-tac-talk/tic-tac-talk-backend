package com.khi.ragservice.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.khi.ragservice.client.UserClient;
import com.khi.ragservice.common.api.ApiResponse;
import com.khi.ragservice.common.exception.ResourceNotFoundException;
import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.ReportSummaryDto;
import com.khi.ragservice.dto.ReportTitleDto;
import com.khi.ragservice.dto.UpdateUserNameRequestDto;
import com.khi.ragservice.dto.UserNicknameRequestDto;
import com.khi.ragservice.dto.UserProfileResponseDto;
import com.khi.ragservice.dto.reportcard.ReportCardDto;
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
        private final UserClient userClient;
        private final ObjectMapper objectMapper;

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
                                entity.getSourceType(),
                                entity.getIsNameUpdated());
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
                                        entity.getSourceType(),
                                        entity.getIsNameUpdated());
                }

                // selectedSpeaker 검증
                String selectedSpeaker = requestDto.getSelectedSpeaker();
                if (!"A".equals(selectedSpeaker) && !"B".equals(selectedSpeaker)) {
                        throw new IllegalArgumentException(
                                        "selectedSpeaker must be 'A' or 'B', but got: " + selectedSpeaker);
                }

                // Fetch logged-in user's nickname from security-service
                log.info("[ReportService] Fetching user profile for userId: {}", userId);
                UserNicknameRequestDto nicknameRequest = new UserNicknameRequestDto(userId);
                ApiResponse<UserProfileResponseDto> userProfileResponse = userClient
                                .getUserNickname(nicknameRequest);
                String loggedInUserName = userProfileResponse.getData().getNickname();
                log.info("[ReportService] Fetched nickname: {}", loggedInUserName);

                // 화자 기반 이름 및 userId 업데이트
                // selectedSpeaker가 "A"면 user1Id를, "B"면 user2Id를 로그인 유저 ID로 업데이트
                if ("A".equals(selectedSpeaker)) {
                        entity.setUser1Id(userId);
                        entity.setUser1Name(loggedInUserName);
                        entity.setUser2Name(requestDto.getOtherUserName());
                        log.info("[ReportService] user1 업데이트 - reportId: {}, user1Id: {}, user1Name: {}, user2Name: {}",
                                        reportId, userId, loggedInUserName, requestDto.getOtherUserName());
                } else {
                        entity.setUser2Id(userId);
                        entity.setUser1Name(requestDto.getOtherUserName());
                        entity.setUser2Name(loggedInUserName);
                        log.info("[ReportService] user2 업데이트 - reportId: {}, user2Id: {}, user1Name: {}, user2Name: {}",
                                        reportId, userId, requestDto.getOtherUserName(), loggedInUserName);
                }
                for (ChatMessageDto message : chatData) {
                        if (selectedSpeaker.equals(message.getName())) {
                                // 로그인 유저가 선택한 화자 → Feign으로 가져온 실제 이름으로 변경
                                message.setName(loggedInUserName);
                        } else {
                                // 나머지 화자 → 상대방 이름으로 변경
                                message.setName(requestDto.getOtherUserName());
                        }
                }

                // reportCards 내부의 A, B 텍스트도 실제 이름으로 치환
                try {
                        List<ReportCardDto> reportCards = entity.getReportCards();
                        if (reportCards != null && !reportCards.isEmpty()) {
                                // reportCards를 JSON 문자열로 변환
                                String reportCardsJson = objectMapper.writeValueAsString(reportCards);
                                log.info("[ReportService] Original reportCards JSON length: {}",
                                                reportCardsJson.length());

                                // 이름 치환: "A" → 실제 이름, "B" → 상대방 이름
                                String nameA, nameB;
                                if ("A".equals(selectedSpeaker)) {
                                        nameA = loggedInUserName;
                                        nameB = requestDto.getOtherUserName();
                                } else {
                                        nameA = requestDto.getOtherUserName();
                                        nameB = loggedInUserName;
                                }

                                // JSON 문자열 내의 "A"와 "B" 치환
                                // "A 님" 형태와 단독 "A" 모두 치환
                                reportCardsJson = reportCardsJson.replace("A 님", nameA + " 님");
                                reportCardsJson = reportCardsJson.replace("B 님", nameB + " 님");
                                reportCardsJson = reportCardsJson.replace("\"A\"", "\"" + nameA + "\"");
                                reportCardsJson = reportCardsJson.replace("\"B\"", "\"" + nameB + "\"");

                                log.info("[ReportService] Replaced A → '{}', B → '{}'", nameA, nameB);

                                // 다시 ReportCardDto 리스트로 변환
                                List<ReportCardDto> updatedReportCards = objectMapper.readValue(
                                                reportCardsJson,
                                                new TypeReference<List<ReportCardDto>>() {
                                                });

                                entity.setReportCards(updatedReportCards);
                                log.info("[ReportService] reportCards 이름 치환 완료");
                        }
                } catch (Exception e) {
                        log.error("[ReportService] reportCards 이름 치환 실패", e);
                        // 치환 실패해도 계속 진행 (chatData와 필드는 이미 업데이트됨)
                }

                // Set isNameUpdated to true when updateUserName is called
                entity.setIsNameUpdated(true);

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
                                savedEntity.getSourceType(),
                                savedEntity.getIsNameUpdated());
        }
}
