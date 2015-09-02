package dk.dbc.dataio.gatekeeper.operation;

public interface Operation {
    void execute() throws OperationExecutionException;
}
