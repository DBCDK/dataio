package dk.dbc.dataio.gui.client.exceptions;

public enum FlowStoreProxyError {
    UNKNOWN,
    KEY_VIOLATION,      // violation of unique key contraints
    DATA_VALIDATION,    // invalid data content
}
