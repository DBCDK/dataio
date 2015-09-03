package dk.dbc.dataio.gatekeeper.operation;

import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;

public class CreateTransfileOperation implements Operation {
    private static final Opcode OPCODE = Opcode.CREATE_TRANSFILE;

    final Path destination;
    final String content;

    public CreateTransfileOperation(Path destination, String content)
            throws NullPointerException, IllegalArgumentException {
        this.destination = InvariantUtil.checkNotNullOrThrow(destination, "destination");
        this.content = InvariantUtil.checkNotNullNotEmptyOrThrow(content, "content");
    }

    @Override
    public Opcode getOpcode() {
        return OPCODE;
    }

    @Override
    public void execute() throws OperationExecutionException {
        try {
            Files.write(destination, content.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE_NEW);
        } catch (IOException e) {
            throw new OperationExecutionException(e);
        }
    }

    public Path getDestination() {
        return destination;
    }

    public String getContent() {
        return content;
    }
}
