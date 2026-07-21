package com.khi.ragservice.entity;

import com.khi.ragservice.dto.ChatMessageDto;
import com.khi.ragservice.dto.reportcard.ReportCardDto;
import com.khi.ragservice.enums.ReportState;
import com.khi.ragservice.enums.SourceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "conversation_reports")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@ToString(of = {"id", "state", "sourceType"})
public class ConversationReport {

    @EqualsAndHashCode.Include
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user1_id", nullable = false)
    private String user1Id;

    @Column(name = "user1_name")
    private String user1Name;

    @Column(name = "user2_id", nullable = false)
    private String user2Id;

    @Column(name = "user2_name")
    private String user2Name;

    @Column(name = "title")
    private String title;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "chat_data", columnDefinition = "jsonb")
    private List<ChatMessageDto> chatData;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "report_cards", columnDefinition = "jsonb")
    private List<ReportCardDto> reportCards;

    @Enumerated(EnumType.STRING)
    @Column(name = "state", nullable = false)
    private ReportState state = ReportState.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(name = "source_type")
    private SourceType sourceType;

    @Column(name = "is_name_updated", nullable = false)
    private Boolean isNameUpdated = false;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (state == null) {
            state = ReportState.PENDING;
        }
    }

    public static ConversationReport pending(String user1Id, String user1Name,
                                              String user2Id, String user2Name, SourceType sourceType) {
        ConversationReport report = new ConversationReport();
        report.user1Id = user1Id;
        report.user1Name = user1Name;
        report.user2Id = user2Id;
        report.user2Name = user2Name;
        report.title = "생성 중...";
        report.sourceType = sourceType;
        report.state = ReportState.PENDING;
        return report;
    }

    public void complete(String title, List<ChatMessageDto> chatData, List<ReportCardDto> reportCards) {
        if (state == ReportState.COMPLETED) {
            return;
        }
        if (state != ReportState.PENDING) {
            throw new IllegalStateException("PENDING 상태에서만 완료 처리할 수 있습니다. 현재 상태: " + state);
        }
        this.title = title;
        this.chatData = chatData;
        this.reportCards = reportCards;
        this.state = ReportState.COMPLETED;
    }

    public void fail() {
        if (state == ReportState.COMPLETED) {
            return;
        }
        this.state = ReportState.FAILED;
    }

    public void resolveSpeaker(String selectedSpeaker, String userId, String loggedInUserName, String otherUserName) {
        if (!"A".equals(selectedSpeaker) && !"B".equals(selectedSpeaker)) {
            throw new IllegalArgumentException("selectedSpeaker must be 'A' or 'B', but got: " + selectedSpeaker);
        }
        if ("A".equals(selectedSpeaker)) {
            this.user1Id = userId;
            this.user1Name = loggedInUserName;
            this.user2Name = otherUserName;
        } else {
            this.user2Id = userId;
            this.user1Name = otherUserName;
            this.user2Name = loggedInUserName;
        }
        this.isNameUpdated = true;
    }

    public void updateReportCards(List<ReportCardDto> reportCards) {
        this.reportCards = reportCards;
    }
}
