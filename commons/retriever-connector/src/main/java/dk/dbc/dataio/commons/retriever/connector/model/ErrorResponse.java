package dk.dbc.dataio.commons.retriever.connector.model;

public record ErrorResponse(int status, String message, String timestamp) {
}
