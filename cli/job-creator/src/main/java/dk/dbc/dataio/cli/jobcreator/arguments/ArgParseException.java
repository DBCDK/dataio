package dk.dbc.dataio.cli.jobcreator.arguments;

public class ArgParseException extends Throwable {
    public ArgParseException(String msg) {
        super(msg);
    }
    public ArgParseException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
