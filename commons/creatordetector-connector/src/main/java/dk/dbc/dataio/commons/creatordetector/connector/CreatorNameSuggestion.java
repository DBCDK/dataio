package dk.dbc.dataio.commons.creatordetector.connector;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_NULL)
public record CreatorNameSuggestion(
    @JsonProperty("detected_ner_name") String detectedNerName,
    @JsonProperty("authority_id") String authorityId,
    @JsonProperty("authority_name_normalized") String authorityNameNormalized,
    @JsonProperty("match_score") double matchScore,
    @JsonProperty("rerank_score") double rerankScore
) {}
