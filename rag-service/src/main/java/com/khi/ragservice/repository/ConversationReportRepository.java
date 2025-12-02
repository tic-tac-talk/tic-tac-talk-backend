package com.khi.ragservice.repository;

import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.enums.ReportState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ConversationReportRepository extends JpaRepository<ConversationReport, Long> {
    List<ConversationReport> findByUser1IdOrUser2Id(String user1Id, String user2Id);

    Page<ConversationReport> findByUser1IdOrUser2Id(String user1Id, String user2Id, Pageable pageable);

    Optional<ConversationReport> findFirstByUser1IdAndUser2IdAndStateOrderByCreatedAtDesc(
            String user1Id, String user2Id, ReportState state);

    @Query(value = "SELECT * FROM conversation_reports c WHERE " +
            "((c.user1_id = :userId1 AND c.user2_id = :userId2) OR " +
            "(c.user1_id = :userId2 AND c.user2_id = :userId1)) AND " +
            "c.state = CAST(:state AS text) ORDER BY c.created_at DESC LIMIT 1", nativeQuery = true)
    Optional<ConversationReport> findFirstByUserIdsAndStateOrderByCreatedAtDesc(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2,
            @Param("state") String state);

    @Modifying
    @Query(value = "INSERT INTO conversation_reports " +
            "(id, user1_id, user1_name, user2_id, user2_name, title, chat_data, report_cards, state, source_type, created_at) "
            +
            "VALUES (:id, :user1Id, :user1Name, :user2Id, :user2Name, :title, CAST(:chatData AS jsonb), CAST(:reportCards AS jsonb), CAST(:state AS text), CAST(:sourceType AS text), CURRENT_TIMESTAMP) "
            +
            "ON CONFLICT (id) DO UPDATE SET " +
            "user1_id = EXCLUDED.user1_id, user1_name = EXCLUDED.user1_name, user2_id = EXCLUDED.user2_id, user2_name = EXCLUDED.user2_name, "
            +
            "title = EXCLUDED.title, chat_data = EXCLUDED.chat_data, report_cards = EXCLUDED.report_cards, state = EXCLUDED.state, source_type = EXCLUDED.source_type", nativeQuery = true)
    void upsertReport(@Param("id") Long id,
            @Param("user1Id") String user1Id,
            @Param("user1Name") String user1Name,
            @Param("user2Id") String user2Id,
            @Param("user2Name") String user2Name,
            @Param("title") String title,
            @Param("chatData") String chatData,
            @Param("reportCards") String reportCards,
            @Param("state") String state,
            @Param("sourceType") String sourceType);
}
