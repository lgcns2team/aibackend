package com.lgcns.haibackend.discussion.domain.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DebateSummaryResponse {

    private Summary summary;

    @JsonProperty("alternative_outcomes")
    private List<AlternativeOutcome> alternativeOutcomes;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Summary {
        @JsonProperty("position_1")
        private Position position1;

        @JsonProperty("position_2")
        private Position position2;

        @JsonProperty("position_3")
        private Position position3;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Position {
        private String stance;

        @JsonProperty("key_argument")
        private String keyArgument;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AlternativeOutcome {
        private String scenario;

        @JsonProperty("short_term_impact")
        private String shortTermImpact;

        @JsonProperty("long_term_impact")
        private String longTermImpact;

        @JsonProperty("historical_implications")
        private String historicalImplications;
    }
}
