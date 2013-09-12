package dk.dbc.dataio.gui.client.exceptions;

public enum FlowStoreProxyError {
    UNKNOWN(1),
    KEY_VIOLATION(2),       // violation of unique key contraints
    DATA_VALIDATION(3);     // invalid data content

    private /* final */ int number;

    private FlowStoreProxyError(int number) {
        this.number = number;
    }

    public int getNumber() {
        return number;
    }
}
