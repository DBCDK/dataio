package dk.dbc.dataio.commons.retriever.connector.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

/**
 * This record encapsulates the result of an articles retrieval operation, providing access
 * to a list of Article objects.
 */
public record ArticlesResponse(
        int total,
        @JsonProperty("documents") List<Article> articles) { }
