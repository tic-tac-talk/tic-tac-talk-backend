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

    @Column(name = "user_id", nullable = true)
    private String userId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_data", columnDefinition = "jsonb", nullable = true)
    private List<ChatMessageDto> chatData;

    @Column(name = "rag_processed")
    private boolean ragProcessed;
}
