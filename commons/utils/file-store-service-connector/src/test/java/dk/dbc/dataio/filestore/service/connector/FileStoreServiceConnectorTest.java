package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpDelete;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Response;
import java.io.InputStream;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FileStoreServiceConnectorTest {
    private static final String FILE_STORE_URL = "http://dataio/file-store";
    private static final String FILE_ID = "42";
    private static final String LOCATION_HEADER = String.format("%s/%s/%s",
            FILE_STORE_URL, FileStoreServiceConstants.FILES_COLLECTION, FILE_ID);
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);
    private final InputStream is = mock(InputStream.class);
    private final FileStoreServiceConnector fileStoreServiceConnector =
            new FileStoreServiceConnector(failSafeHttpClient, FILE_STORE_URL);

    private final HttpGet getFileRequest = new HttpGet(failSafeHttpClient)
            .withHeader("Accept-Encoding", "")
            .withBaseUrl(FILE_STORE_URL)
            .withPathElements(
                    new PathBuilder(FileStoreServiceConstants.FILE)
                            .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID)
                            .build());

    private final HttpGet getByteSizeRequest = new HttpGet(failSafeHttpClient)
            .withHeader("Accept-Encoding", "")
            .withBaseUrl(FILE_STORE_URL)
            .withPathElements(
                    new PathBuilder(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
                            .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID)
                            .build());

    private final HttpDelete deleteFileRequest = new HttpDelete(failSafeHttpClient)
            .withBaseUrl(FILE_STORE_URL)
            .withPathElements(
                    new PathBuilder(FileStoreServiceConstants.FILE)
                            .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID)
                            .build());

    @Test
    public void addFile_isArgIsNull_throws() {
        assertThat(() -> fileStoreServiceConnector.addFile(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void addFile_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        try {
            fileStoreServiceConnector.addFile(is);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void addFile_responseWithoutLocationHeader_throws() {
        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), ""));

        try {
            fileStoreServiceConnector.addFile(is);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorException e) {
            assertThat(e instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test
    public void addFile_fileIsCreated_returnsFileId() throws FileStoreServiceConnectorException {
        final MockedResponse response = new MockedResponse<>(Response.Status.CREATED.getStatusCode(), "")
                .addHeaderValue("Location", LOCATION_HEADER);

        when(failSafeHttpClient.execute(any(HttpPost.class)))
                .thenReturn(response);

        assertThat(fileStoreServiceConnector.addFile(is), is(FILE_ID));
    }

    @Test
    public void getFile_fileIdArgIsNull_throws() {
        assertThat(() -> fileStoreServiceConnector.getFile(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void getFile_fileIdArgIsEmpty_throws() {
        assertThat(() -> fileStoreServiceConnector.getFile(" "), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void getFile_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        when(failSafeHttpClient.execute(getFileRequest))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        try {
            fileStoreServiceConnector.getFile(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void getFile_responseWithNullEntity_throws() {
        when(failSafeHttpClient.execute(getFileRequest))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        try {
            fileStoreServiceConnector.getFile(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorException e) {
            assertThat(e instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test
    public void getFile_fileExists_returnsInputStream() throws FileStoreServiceConnectorException {
        when(failSafeHttpClient.execute(getFileRequest))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), is));

        assertThat(fileStoreServiceConnector.getFile(FILE_ID), is(is));
    }

    @Test
    public void deleteFile_fileIdArgIsNull_throws() {
        assertThat(() -> fileStoreServiceConnector.deleteFile((String) null), isThrowing(NullPointerException.class));
    }

    @Test
    public void deleteFile_fileIdArgIsEmpty_throws() {
        assertThat(() -> fileStoreServiceConnector.deleteFile(""), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void deleteFile_onProcessingException_throws() {
        when(failSafeHttpClient.execute(deleteFileRequest))
                .thenThrow(new ProcessingException("Connection reset"));

        assertThat(() -> fileStoreServiceConnector.deleteFile(FILE_ID), isThrowing(FileStoreServiceConnectorException.class));
    }

    @Test
    public void deleteFile_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        when(failSafeHttpClient.execute(deleteFileRequest))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        try {
            fileStoreServiceConnector.deleteFile(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void deleteFile_serviceReturnsStatusOk_returns() throws FileStoreServiceConnectorException {
        when(failSafeHttpClient.execute(deleteFileRequest))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), ""));

        fileStoreServiceConnector.deleteFile(FILE_ID);
    }

    @Test
    public void getByteSize_fileIdArgIsNull_throws() {
        assertThat(() -> fileStoreServiceConnector.getByteSize(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void getByteSize_fileIdArgIsEmpty_throws() {
        assertThat(() -> fileStoreServiceConnector.getByteSize(" "), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void getByteSize_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        when(failSafeHttpClient.execute(getByteSizeRequest))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        try {
            fileStoreServiceConnector.getByteSize(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void getByteSize_responseWithNullEntity_throws() {
        when(failSafeHttpClient.execute(getByteSizeRequest))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        try {
            fileStoreServiceConnector.getByteSize(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorException e) {
            assertThat(e instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test
    public void getByteSize_fileAttributesExists_returnsByteSize() throws FileStoreServiceConnectorException {
        when(failSafeHttpClient.execute(getByteSizeRequest))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), 42L));

        assertThat(fileStoreServiceConnector.getByteSize(FILE_ID), is(42L));
    }
}
