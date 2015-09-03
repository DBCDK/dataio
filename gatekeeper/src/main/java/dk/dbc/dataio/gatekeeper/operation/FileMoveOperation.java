package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileMoveOperation implements Operation {
    private static final Opcode OPCODE = Opcode.MOVE_FILE;

    final Path source;
    final Path destination;

    public FileMoveOperation(Path source, Path destination) throws NullPointerException {
        this.source = InvariantUtil.checkNotNullOrThrow(source, "source");
        this.destination = InvariantUtil.checkNotNullOrThrow(destination, "destination");
    }

    @Override
    public Opcode getOpcode() {
        return OPCODE;
    }

    @Override
    public void execute() throws OperationExecutionException {
        if (Files.exists(source)) {
            try {
                Files.move(source, destination, java.nio.file.StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException e) {
                throw new OperationExecutionException(e);
            }
        }
    }

    public Path getSource() {
        return source;
    }

    public Path getDestination() {
        return destination;
    }
}
