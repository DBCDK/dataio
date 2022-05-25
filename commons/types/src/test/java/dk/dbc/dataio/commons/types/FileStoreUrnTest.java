package dk.dbc.dataio.commons.types;

import org.junit.Test;

import java.net.URISyntaxException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class FileStoreUrnTest {
    private final String fileId = "42";

    @Test(expected = NullPointerException.class)
    public void constructor_urnStringArgIsNull_throws() throws URISyntaxException {
        new FileStoreUrn(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_urnStringArgIsEmpty_throws() throws URISyntaxException {
        new FileStoreUrn("");
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgIsInvalidUri_throws() throws URISyntaxException {
        new FileStoreUrn("1:2:3");
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasInvalidScheme_throws() throws URISyntaxException {
        new FileStoreUrn("uri:type:42");
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasInvalidType_throws() throws URISyntaxException {
        new FileStoreUrn(String.format("%s:type:42", FileStoreUrn.SCHEME));
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasNullFileId_throws() throws URISyntaxException {
        new FileStoreUrn(String.format("%s:%s", FileStoreUrn.SCHEME, FileStoreUrn.TYPE));
    }

    @Test(expected = URISyntaxException.class)
    public void constructor_urnStringArgHasEmptyFileId_throws() throws URISyntaxException {
        new FileStoreUrn(String.format("%s:%s: ", FileStoreUrn.SCHEME, FileStoreUrn.TYPE));
    }

    @Test
    public void constructor_urnStringArgIsValid_returnsNewInstance() throws URISyntaxException {
        final FileStoreUrn fileStoreUrn = new FileStoreUrn(String.format("%s:%s:%s", FileStoreUrn.SCHEME, FileStoreUrn.TYPE, fileId));
        assertThat(fileStoreUrn, is(notNullValue()));
        assertThat(fileStoreUrn.getFileId(), is(fileId));
    }

    @Test(expected = NullPointerException.class)
    public void create_fileIdArgIsNull_throws() {
        FileStoreUrn.create(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void create_fileIdArgIsEmpty_throws() {
        FileStoreUrn.create("");
    }

    @Test
    public void create_fileIdArgIsValid_returnsNewInstance() {
        final FileStoreUrn fileStoreUrn = FileStoreUrn.create(fileId);
        assertThat(fileStoreUrn, is(notNullValue()));
        assertThat(fileStoreUrn.getFileId(), is(fileId));
    }
}
