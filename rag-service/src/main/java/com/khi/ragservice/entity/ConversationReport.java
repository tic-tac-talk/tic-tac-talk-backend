package com.khi.ragservice.entity;

import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.reportcard.ReportCardDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversation_reports")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationReport {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private String user1Id;

    @Column(name = "user2_id", nullable = false)
    private String user2Id;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_data", columnDefinition = "jsonb", nullable = false)
    private List<ChatMessageDto> chatData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_cards", columnDefinition = "jsonb", nullable = false)
    private List<ReportCardDto> reportCards;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
    }
}
