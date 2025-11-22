package com.khi.voiceservice.dto.reportcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class MistakesCardDto extends ReportCardDto {
    private MistakesContent content;

    @Data
    public static class MistakesContent {
        private List<Mistake> mistakes;
    }

    @Data
    public static class Mistake {
        private String type;
        private String definition;
        private Boolean participantA;
        private Boolean participantB;
        private String severity; // "low", "medium", "high"
        private String evidence;
    }
}
