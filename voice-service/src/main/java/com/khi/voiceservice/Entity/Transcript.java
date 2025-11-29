package com.khi.voiceservice.Entity;

import com.khi.voiceservice.dto.ChatMessageDto;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.util.List;

@Entity
@Table(name = "transcript")
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Transcript {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_1_id", nullable = false)
    private String user1Id;

    @Column(name = "user_2_id", nullable = false)
    private String user2Id;

    // Rag 분석 요청 여부 (중복 요청 방지)
    @Column(name = "clova_processed")
    private boolean clovaProcessed;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_data", columnDefinition = "jsonb", nullable = true)
    private List<ChatMessageDto> chatData;

    @Column(name = "conversation_report_id", nullable = true)
    private Long conversationReportId;
}
