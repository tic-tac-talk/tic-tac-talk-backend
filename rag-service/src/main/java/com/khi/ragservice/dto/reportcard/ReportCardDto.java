package com.khi.ragservice.dto.reportcard;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import lombok.Data;

@Data
@JsonTypeInfo(
    use = JsonTypeInfo.Id.NAME,
    include = JsonTypeInfo.As.EXISTING_PROPERTY,
    property = "type",
    visible = true
)
@JsonSubTypes({
    @JsonSubTypes.Type(value = SummaryCardDto.class, name = "summary"),
    @JsonSubTypes.Type(value = AnalysisCardDto.class, name = "analysis"),
    @JsonSubTypes.Type(value = BehaviorCardDto.class, name = "behavior"),
    @JsonSubTypes.Type(value = MistakesCardDto.class, name = "mistakes"),
    @JsonSubTypes.Type(value = CoachingCardDto.class, name = "coaching"),
    @JsonSubTypes.Type(value = RatioCardDto.class, name = "ratio")
})
public abstract class ReportCardDto {
    private String id;
    private String title;
    private String type;
}
