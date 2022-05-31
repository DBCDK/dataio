package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileDeleteOperation implements Operation {
    private static final Opcode OPCODE = Opcode.DELETE_FILE;

    final Path file;

    public FileDeleteOperation(Path file) throws NullPointerException {
        this.file = InvariantUtil.checkNotNullOrThrow(file, "file");
    }

    @Override
    public Opcode getOpcode() {
        return OPCODE;
    }

    @Override
    public void execute() throws OperationExecutionException {
        try {
            Files.deleteIfExists(file);
        } catch (IOException e) {
            throw new OperationExecutionException(e);
        }
    }

    public Path getFile() {
        return file;
    }
}
