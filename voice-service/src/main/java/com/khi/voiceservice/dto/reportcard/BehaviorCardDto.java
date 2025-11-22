package com.khi.voiceservice.dto.reportcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.List;

@Data
@EqualsAndHashCode(callSuper = true)
public class BehaviorCardDto extends ReportCardDto {
    private BehaviorContent content;

    @Data
    public static class BehaviorContent {
        private List<BehaviorItem> biases;
        private List<BehaviorItem> skills;
    }

    @Data
    public static class BehaviorItem {
        private String title;
        private String description;
    }
}
