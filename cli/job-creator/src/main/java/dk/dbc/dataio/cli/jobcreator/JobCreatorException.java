package dk.dbc.dataio.cli.jobcreator;

public class JobCreatorException extends Throwable {
    public JobCreatorException(String msg) {
        super(msg);
    }
    public JobCreatorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
