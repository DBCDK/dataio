/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Response;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
})
public class FileStoreServiceConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String FILE_STORE_URL = "http://dataio/file-store";
    private static final InputStream INPUT_STREAM = mock(InputStream.class);
    private static final String FILE_ID = "42";
    private static final String LOCATION_HEADER = String.format("%s/%s/%s",
            FILE_STORE_URL, FileStoreServiceConstants.FILES_COLLECTION, FILE_ID);

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new FileStoreServiceConnector(null, FILE_STORE_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new FileStoreServiceConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new FileStoreServiceConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final FileStoreServiceConnector instance = newFileStoreServiceConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(FILE_STORE_URL));
    }

    @Test
    public void addFile_isArgIsNull_throws() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getFile(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addFile_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        when(HttpClient.doPost(eq(CLIENT), any(Entity.class), eq(FILE_STORE_URL), eq(FileStoreServiceConstants.FILES_COLLECTION)))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.addFile(INPUT_STREAM);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void addFile_responseWithoutLocationHeader_throws() throws FileStoreServiceConnectorException {
        when(HttpClient.getHeader(any(Response.class), eq("Location"))).thenReturn(Collections.emptyList());
        when(HttpClient.doPost(eq(CLIENT), any(Entity.class), eq(FILE_STORE_URL), eq(FileStoreServiceConstants.FILES_COLLECTION)))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), ""));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.addFile(INPUT_STREAM);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorException e) {
            assertThat(e instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test
    public void addFile_fileIsCreated_returnsFileId() throws FileStoreServiceConnectorException {
        when(HttpClient.getHeader(any(Response.class), eq("Location"))).thenReturn(Arrays.<Object>asList(LOCATION_HEADER));
        when(HttpClient.doPost(eq(CLIENT), any(Entity.class), eq(FILE_STORE_URL), eq(FileStoreServiceConstants.FILES_COLLECTION)))
                .thenReturn(new MockedResponse<>(Response.Status.CREATED.getStatusCode(), ""));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        assertThat(fileStoreServiceConnector.addFile(INPUT_STREAM), is(FILE_ID));
    }

    @Test
    public void getFile_fileIdArgIsNull_throws() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getFile(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getFile_fileIdArgIsEmpty_throws() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getFile("");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void getFile_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doGet(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getFile(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void getFile_responseWithNullEntity_throws() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doGet(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getFile(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorException e) {
            assertThat(e instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test
    public void getFile_fileExists_returnsInputStream() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doGet(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), INPUT_STREAM));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        assertThat(fileStoreServiceConnector.getFile(FILE_ID), is(INPUT_STREAM));
    }

    @Test
    public void deleteFile_fileIdArgIsNull_throws() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.deleteFile(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void deleteFile_fileIdArgIsEmpty_throws() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.deleteFile("");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void deleteFile_onProcessingException_throws() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doDelete(CLIENT, FILE_STORE_URL, path.build()))
                .thenThrow(new ProcessingException("Connection reset"));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.deleteFile(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorException e) {
        }
    }

    @Test
    public void deleteFile_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doDelete(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.deleteFile(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void deleteFile_serviceReturnsStatusOk_returns() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doDelete(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), ""));

        newFileStoreServiceConnector().deleteFile(FILE_ID);
    }

    //********

    @Test
    public void getByteSize_fileIdArgIsNull_throws() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getByteSize(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void getByteSize_fileIdArgIsEmpty_throws() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getByteSize("");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void getByteSize_responseWithUnexpectedStatusCode_throws() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doGet(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), ""));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getByteSize(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        }
    }

    @Test
    public void getByteSize_responseWithNullEntity_throws() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doGet(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), null));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        try {
            fileStoreServiceConnector.getByteSize(FILE_ID);
            fail("No exception thrown");
        } catch (FileStoreServiceConnectorException e) {
            assertThat(e instanceof FileStoreServiceConnectorUnexpectedStatusCodeException, is(false));
        }
    }

    @Test
    public void getByteSize_fileAttributesExists_returnsByteSize() throws FileStoreServiceConnectorException {
        final PathBuilder path = new PathBuilder(FileStoreServiceConstants.FILE_ATTRIBUTES_BYTESIZE)
                .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, FILE_ID);
        when(HttpClient.doGet(CLIENT, FILE_STORE_URL, path.build()))
                .thenReturn(new MockedResponse<>(Response.Status.OK.getStatusCode(), 42L));

        final FileStoreServiceConnector fileStoreServiceConnector = newFileStoreServiceConnector();
        assertThat(fileStoreServiceConnector.getByteSize(FILE_ID), is(42L));
    }



    private static FileStoreServiceConnector newFileStoreServiceConnector() {
        return new FileStoreServiceConnector(CLIENT, FILE_STORE_URL);
    }
}