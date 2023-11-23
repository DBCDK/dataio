package dk.dbc.dataio.bfs.api;

import org.junit.Test;

import java.nio.file.Path;
import java.nio.file.Paths;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class BinaryFileStoreFsImplTest {
    private static final Path BASE_PATH = Paths.get("/absolute");

    @Test(expected = NullPointerException.class)
    public void constructor_baseArgIsNull_throws() {
        new BinaryFileStoreFsImpl(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseArgIsNonAbsolute() {
        new BinaryFileStoreFsImpl(Paths.get("non-absolute"));
    }

    @Test
    public void constructor_baseArgIsValid_returnsNewInstance() {
        final BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        assertThat(binaryFileStoreFs, is(notNullValue()));
    }

    @Test
    public void getBinaryFile_pathArgIsNull_throws() {
        final BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        try {
            binaryFileStoreFs.getBinaryFile(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getBinaryFile_pathArgIsAbsolute_throws() {
        final Path filePath = Paths.get("/also/absolute");
        final BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        try {
            binaryFileStoreFs.getBinaryFile(filePath);
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void getBinaryFile_PathArgIsValid_returnsBinaryFileRepresentation() {
        final Path filePath = Paths.get("path/to/file");
        final BinaryFileStoreFsImpl binaryFileStoreFs = new BinaryFileStoreFsImpl(BASE_PATH);
        final BinaryFile binaryFile = binaryFileStoreFs.getBinaryFile(filePath);
        assertThat(binaryFile, is(notNullValue()));
        assertThat(binaryFile.getPath(), is(BASE_PATH.resolve(filePath)));
    }
}
