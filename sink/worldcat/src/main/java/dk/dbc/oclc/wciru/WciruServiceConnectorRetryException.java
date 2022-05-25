package dk.dbc.oclc.wciru;


public class WciruServiceConnectorRetryException extends WciruServiceConnectorException {
    private int numberOfRetries = 0;

    public WciruServiceConnectorRetryException(String message) {
        super(message);
    }

    public WciruServiceConnectorRetryException(String message, Exception cause) {
        super(message, cause);
    }

    public WciruServiceConnectorRetryException(String message, Diagnostic diagnostic, int numberOfRetries) {
        super(message, diagnostic);
        this.numberOfRetries = numberOfRetries;
    }

    public int getNumberOfRetries() {
        return numberOfRetries;
    }
}
