package dk.dbc.dataio.bfs.api;

import org.junit.jupiter.api.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BinaryFileStoreFsImplTest {
    private static final Path BASE_PATH = Paths.get("/absolute");

    @Test
    public void constructor_baseArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new BinaryFileStoreFsImpl(null));
    }

    @Test
    public void constructor_baseArgIsNonAbsolute() {
        assertThrows(IllegalArgumentException.class, () -> new BinaryFileStoreFsImpl(Paths.get("non-absolute")));
    }

    @Test
    public void constructor_baseArgIsValid_returnsNewInstance() {
        BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        assertThat(binaryFileStoreFs, is(notNullValue()));
    }

    @Test
    public void getBinaryFile_pathArgIsNull_throws() {
        BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        assertThrows(NullPointerException.class, () -> binaryFileStoreFs.getBinaryFile(null));
    }

    @Test
    public void getBinaryFile_pathArgIsAbsolute_throws() {
        Path filePath = Paths.get("/also/absolute");
        BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        assertThrows(IllegalArgumentException.class, () -> binaryFileStoreFs.getBinaryFile(filePath));
    }

    @Test
    public void getBinaryFile_PathArgIsValid_returnsBinaryFileRepresentation() {
        Path filePath = Paths.get("path/to/file");
        BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        BinaryFile binaryFile = binaryFileStoreFs.getBinaryFile(filePath);
        assertThat(binaryFile, is(notNullValue()));
        assertThat(binaryFile.getPath(), is(BASE_PATH.resolve(filePath)));
    }
}
