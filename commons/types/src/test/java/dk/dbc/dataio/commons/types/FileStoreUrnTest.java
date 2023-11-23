package dk.dbc.dataio.commons.types;

import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class FileStoreUrnTest {
    private final String fileId = "42";

    @Test
    public void constructor_urnStringArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new FileStoreUrn(null));
    }

    @Test
    public void constructor_urnStringArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new FileStoreUrn(""));
    }

    @Test
    public void constructor_urnStringArgIsInvalidUri_throws() {
        assertThrows(URISyntaxException.class, () -> new FileStoreUrn("1:2:3"));
    }

    @Test
    public void constructor_urnStringArgHasInvalidScheme_throws() {
        assertThrows(URISyntaxException.class, () -> new FileStoreUrn("uri:type:42"));
    }

    @Test
    public void constructor_urnStringArgHasInvalidType_throws() {
        assertThrows(URISyntaxException.class, () -> new FileStoreUrn(String.format("%s:type:42", FileStoreUrn.SCHEME)));
    }

    @Test
    public void constructor_urnStringArgHasNullFileId_throws() {
        assertThrows(URISyntaxException.class, () -> new FileStoreUrn(String.format("%s:%s", FileStoreUrn.SCHEME, FileStoreUrn.TYPE)));
    }

    @Test
    public void constructor_urnStringArgHasEmptyFileId_throws() {
        assertThrows(URISyntaxException.class, () -> new FileStoreUrn(String.format("%s:%s: ", FileStoreUrn.SCHEME, FileStoreUrn.TYPE)));
    }

    @Test
    public void constructor_urnStringArgIsValid_returnsNewInstance() throws URISyntaxException {
        FileStoreUrn fileStoreUrn = new FileStoreUrn(String.format("%s:%s:%s", FileStoreUrn.SCHEME, FileStoreUrn.TYPE, fileId));
        assertThat(fileStoreUrn, is(notNullValue()));
        assertThat(fileStoreUrn.getFileId(), is(fileId));
    }

    @Test
    public void create_fileIdArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> FileStoreUrn.create(null));
    }

    @Test
    public void create_fileIdArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> FileStoreUrn.create(""));
    }

    @Test
    public void create_fileIdArgIsValid_returnsNewInstance() {
        FileStoreUrn fileStoreUrn = FileStoreUrn.create(fileId);
        assertThat(fileStoreUrn, is(notNullValue()));
        assertThat(fileStoreUrn.getFileId(), is(fileId));
    }
}
