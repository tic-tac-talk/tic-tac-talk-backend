package com.khi.ragservice.dto.reportcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class AnalysisCardDto extends ReportCardDto {
    private AnalysisContent content;

    @Data
    public static class AnalysisContent {
        private String emotionA;
        private String emotionB;
        private String toneA;
        private String toneB;
        private String overall;
        private String argumentA;
        private String evidenceA;
        private String argumentB;
        private String evidenceB;
        private String errorA;
        private String errorB;
    }
}
