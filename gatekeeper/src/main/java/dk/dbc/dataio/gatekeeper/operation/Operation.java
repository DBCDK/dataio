package dk.dbc.dataio.gatekeeper.operation;

public interface Operation {
    Opcode getOpcode();

    void execute() throws OperationExecutionException;
}
