package com.khi.ragservice.dto.reportcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SummaryCardDto extends ReportCardDto {
    private SummaryContent content;

    @Data
    public static class SummaryContent {
        private String summary;
        private String participantA;
        private String participantB;
    }
}
