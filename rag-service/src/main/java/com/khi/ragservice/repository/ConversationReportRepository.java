package com.khi.ragservice.repository;

import com.khi.ragservice.entity.ConversationReport;
import com.khi.ragservice.enums.ReportState;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
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

    @Query("SELECT c FROM ConversationReport c WHERE " +
            "((c.user1Id = :userId1 AND c.user2Id = :userId2) OR " +
            "(c.user1Id = :userId2 AND c.user2Id = :userId1)) AND " +
            "c.state = :state ORDER BY c.createdAt DESC")
    Optional<ConversationReport> findFirstByUserIdsAndStateOrderByCreatedAtDesc(
            @Param("userId1") String userId1,
            @Param("userId2") String userId2,
            @Param("state") ReportState state);
}
