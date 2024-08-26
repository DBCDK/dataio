package dk.dbc.dataio.filestore.service.ejb;

import dk.dbc.dataio.filestore.service.entity.FileAttributes;
import jakarta.ejb.EJBException;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Paths;
import java.util.Date;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FilesBeanTest {
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final UriBuilder uriBuilder = mock(UriBuilder.class);
    private final FileStoreBean fileStoreBean = mock(FileStoreBean.class);
    private final InputStream inputStream = mock(InputStream.class);
    private final OutputStream outputStream = mock(OutputStream.class);
    private final String fileId = "fileId";

    @Test
    public void addFile_fileIsCreated_returnsStatusCreatedResponse() throws IOException {
        when(fileStoreBean.addFile(inputStream)).thenReturn(fileId);
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(fileId)).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(null);

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.addFile(uriInfo, inputStream);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
    }

    @Test
    public void appendToFile_fileDoesNotExist_returnsStatusNotFoundResponse() {
        when(fileStoreBean.fileExists(fileId)).thenReturn(false);

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.appendToFile(fileId, new ByteArrayInputStream(new byte[0]));
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void addMetadata_returnsCreatedResponse() {
        FileAttributes fileAttributes = new FileAttributes(new Date(),
                Paths.get("path"));
        when(fileStoreBean.addMetaData(anyString(), anyString()))
                .thenReturn(fileAttributes);

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.addMetadata("123456",
                "{\"meta\": \"data\"}");
        assertThat("status", response.getStatus(), is(
                Response.Status.OK.getStatusCode()));
    }

    @Test
    public void getFile_fileExists_returnsStatusOkResponse() {
        when(fileStoreBean.fileExists(fileId)).thenReturn(true);
        doNothing().when(fileStoreBean).getFile(fileId, outputStream, false);

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.getFile(null, fileId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void getFile_fileDoesNotExist_returnsStatusNotFoundResponse() {
        when(fileStoreBean.fileExists(fileId)).thenReturn(false);

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.getFile(null, fileId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getByteSize_fileAttributesNotFound_returnsStatusNotFoundResponse() throws IllegalArgumentException {
        when(fileStoreBean.getByteSize(fileId, true)).thenThrow(new EJBException());

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.getByteSize(null, fileId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getByteSize_fileIdNotANumber_returnsBadRequestResponse() throws IllegalArgumentException {
        when(fileStoreBean.getByteSize(anyString(), eq(true))).thenThrow(new IllegalArgumentException());

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.getByteSize(null, "notANumber");
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void getByteSize_fileAttributesFound_returnsStatusOkResponse() throws IllegalArgumentException {
        long byteSize = 42;
        when(fileStoreBean.getByteSize(fileId, true)).thenReturn(byteSize);

        FilesBean filesBean = newFilesBeanInstance();
        Response response = filesBean.getByteSize(null, fileId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.getEntity(), is(byteSize));
    }

    private FilesBean newFilesBeanInstance() {
        FilesBean filesBean = new FilesBean();
        filesBean.fileStore = fileStoreBean;
        return filesBean;
    }

}
