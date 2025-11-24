package com.khi.ragservice.dto.reportcard;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RatioCardDto extends ReportCardDto {
    private RatioContent content;

    @Data
    public static class RatioContent {
        private Double ratioA;
        private Double ratioB;
        private String reasonA;
        private String reasonB;
    }
}
