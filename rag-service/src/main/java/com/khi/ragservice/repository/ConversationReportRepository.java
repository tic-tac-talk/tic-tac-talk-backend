package com.khi.ragservice.repository;

import com.khi.ragservice.entity.ConversationReport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ConversationReportRepository extends JpaRepository<ConversationReport, Long> {
    List<ConversationReport> findByUser1IdOrUser2Id(String user1Id, String user2Id);
}
