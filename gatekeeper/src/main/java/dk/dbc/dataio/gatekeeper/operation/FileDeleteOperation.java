package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.invariant.InvariantUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

public class FileDeleteOperation implements Operation {
    private static final Logger LOGGER = LoggerFactory.getLogger(FileDeleteOperation.class);
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
            boolean b = Files.deleteIfExists(file);
            if(!b) LOGGER.warn("Unable to delete file {}", file);
        } catch (IOException e) {
            throw new OperationExecutionException(e);
        }
    }

    public Path getFile() {
        return file;
    }
}
