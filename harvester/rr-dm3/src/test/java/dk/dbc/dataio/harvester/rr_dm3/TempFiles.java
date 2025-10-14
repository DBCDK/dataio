package dk.dbc.dataio.harvester.rr_dm3;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;

public interface TempFiles {
    default Path createPath(Path parent) throws IOException {
        return Files.createFile(parent.resolve(UUID.randomUUID() + ".tmp"));
    }

    default File createFile(Path parent) throws IOException {
        return createPath(parent).toFile();
    }

    default Path createDir(Path parent) throws IOException {
        return Files.createDirectory(parent.resolve(UUID.randomUUID().toString()));
    }
}
