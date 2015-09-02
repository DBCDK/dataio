package dk.dbc.dataio.gatekeeper.operation;

public class OperationExecutionException extends Exception {
    private static final long serialVersionUID = -554969559587451997L;

    public OperationExecutionException(Exception e) {
        super(e);
    }
}
