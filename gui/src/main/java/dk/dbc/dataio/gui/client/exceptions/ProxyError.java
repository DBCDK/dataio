package dk.dbc.dataio.gui.client.exceptions;

public enum ProxyError {
    SERVICE_NOT_FOUND,
    BAD_REQUEST,                // invalid data content
    NOT_ACCEPTABLE,             // violation of unique key contraints
    ENTITY_NOT_FOUND,
    CONFLICT_ERROR,             // Concurrent Update Error
    INTERNAL_SERVER_ERROR,
}
