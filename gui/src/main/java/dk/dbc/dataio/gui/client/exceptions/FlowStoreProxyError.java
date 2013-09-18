package dk.dbc.dataio.gui.client.exceptions;

public enum FlowStoreProxyError {
    KEY_CONFLICT,               // violation of unique key contraints
    DATA_NOT_ACCEPTABLE,        // invalid data content
    ENTITY_GONE,
    INTERNAL_SERVER_ERROR,
}
