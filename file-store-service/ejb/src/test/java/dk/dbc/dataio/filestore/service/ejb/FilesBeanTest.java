package dk.dbc.dataio.filestore.service.ejb;

import com.sun.media.sound.InvalidDataException;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
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

        final FilesBean filesBean = newFilesBeanInstance();
        final Response response = filesBean.addFile(uriInfo, inputStream);
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
    }

    @Test
    public void getFile_fileExists_returnsStatusOkResponse() {
        when(fileStoreBean.fileExists(fileId)).thenReturn(true);
        doNothing().when(fileStoreBean).getFile(fileId, outputStream);

        final FilesBean filesBean = newFilesBeanInstance();
        final Response response = filesBean.getFile(fileId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }

    @Test
    public void getFile_fileDoesNotExist_returnsStatusNotFoundResponse() {
        when(fileStoreBean.fileExists(fileId)).thenReturn(false);

        final FilesBean filesBean = newFilesBeanInstance();
        final Response response = filesBean.getFile(fileId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getByteSize_fileAttributesNotFound_returnsStatusNotFoundResponse() throws InvalidDataException{
        when(fileStoreBean.getByteSize(fileId)).thenThrow(new EJBException());

        final FilesBean filesBean = newFilesBeanInstance();
        final Response response = filesBean.getByteSize(fileId);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void getByteSize_fileIdNotANumber_returnsBadRequestResponse() throws InvalidDataException{
        when(fileStoreBean.getByteSize(anyString())).thenThrow(new InvalidDataException());

        final FilesBean filesBean = newFilesBeanInstance();
        final Response response = filesBean.getByteSize("notANumber");
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void getByteSize_fileAttributesFound_returnsStatusOkResponse() throws InvalidDataException {
        long byteSize = 42;
        when(fileStoreBean.getByteSize(fileId)).thenReturn(byteSize);

        final FilesBean filesBean = newFilesBeanInstance();
        final Response response = filesBean.getByteSize(fileId);
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat((Long) response.getEntity(), is(byteSize));
    }

    private FilesBean newFilesBeanInstance() {
        final FilesBean filesBean = new FilesBean();
        filesBean.fileStore = fileStoreBean;
        return filesBean;
    }

}