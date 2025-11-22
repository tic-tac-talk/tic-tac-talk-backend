package com.khi.voiceservice.dto.reportcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class CoachingCardDto extends ReportCardDto {
    private CoachingContent content;

    @Data
    public static class CoachingContent {
        private List<String> adviceA;
        private List<String> adviceB;
    }
}
