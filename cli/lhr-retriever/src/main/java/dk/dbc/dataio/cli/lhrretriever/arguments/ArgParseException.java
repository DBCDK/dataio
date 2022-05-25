package dk.dbc.dataio.cli.lhrretriever.arguments;

public class ArgParseException extends Exception {
    public ArgParseException(String msg) {
        super(msg);
    }

    public ArgParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
