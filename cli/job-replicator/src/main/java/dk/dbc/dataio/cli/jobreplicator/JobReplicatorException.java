package dk.dbc.dataio.cli.jobreplicator;

public class JobReplicatorException extends Throwable {
    public JobReplicatorException(String msg) {
        super(msg);
    }

    public JobReplicatorException(String msg, Throwable cause) {
        super(msg, cause);
    }
}
