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

package dk.dbc.dataio.filestore;

import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.integrationtest.ITUtil;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.PathBuilder;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static junitx.framework.FileAssert.assertBinaryEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

public class FilesIT {
    private static final int MB = 1024*1024;
    private static final int BUFFER_SIZE = 8192;

    private static Client restClient;

    @Rule
    public TemporaryFolder rootFolder = new TemporaryFolder(new File(System.getProperty("build.dir")));

    @BeforeClass
    public static void setUpClass() {
        restClient = newRestClient();
    }

    @AfterClass
    public static void tearDownClass() {
        tearDownRestClient();
        ITUtil.clearFileStore();
    }

    /**
     * Given: a deployed file-store service
     * When: adding a new file of a size larger than the maximum heap size
     * Then: new file can be retrieved by id
     */
    @Test
    public void fileAddedAndRetrieved() throws IOException, FileStoreServiceConnectorException {
        // When...
        final long veryLargeFileSizeInBytes = 1024 * MB; // 1 GB
        final File sourceFile = rootFolder.newFile();
        if (sourceFile.getUsableSpace() < veryLargeFileSizeInBytes * 3) {
            // We need enough space for
            //  1. source file
            //  2. file when uploaded to file store
            //  3. file when read back from file store
            fail("Not enough free space for test: " + (veryLargeFileSizeInBytes * 3) / MB + " MB needed");
        }
        createSparseFile(sourceFile, veryLargeFileSizeInBytes);

        final FileStoreServiceConnector fileStoreServiceConnector =
                new FileStoreServiceConnector(restClient, ITUtil.FILE_STORE_BASE_URL);

        try (final InputStream is = getInputStreamForFile(sourceFile.toPath())) {
            final String fileId = fileStoreServiceConnector.addFile(is);

            // Then...
            final InputStream fileStream = fileStoreServiceConnector.getFile(fileId);
            final Path destinationFile = rootFolder.newFile().toPath();
            writeFile(destinationFile, fileStream);
            assertBinaryEquals(sourceFile, destinationFile.toFile());
        }
    }

    @Test
    public void appendToFile() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector =
                new FileStoreServiceConnector(restClient, ITUtil.FILE_STORE_BASE_URL);
        final String fileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("1"));
        fileStoreServiceConnector.appendToFile(fileId, StringUtil.asBytes("234567"));
        fileStoreServiceConnector.appendToFile(fileId, StringUtil.asBytes("89"));

        assertThat("file content", StringUtil.asString(fileStoreServiceConnector.getFile(fileId)),
                is("123456789"));
        assertThat("file size", fileStoreServiceConnector.getByteSize(fileId),
                is(9L));
    }

    /**
     * Given: a deployed file-store service
     * When : adding a file
     * Then : the input streams byte size has been added to file attributes and the byte size can be retrieved
     */
    @Test
    public void getByteSize() throws IOException, FileStoreServiceConnectorException {
        // When...
        byte[] data = "1234".getBytes(StandardCharsets.UTF_8);

        final FileStoreServiceConnector fileStoreServiceConnector =
                new FileStoreServiceConnector(restClient, ITUtil.FILE_STORE_BASE_URL);

        final InputStream inputStream = new ByteArrayInputStream(data);
        final String fileId = fileStoreServiceConnector.addFile(inputStream);

            // Then...
        final long byteSize = fileStoreServiceConnector.getByteSize(fileId);
        assertThat(byteSize, is((long)data.length));
    }

    /**
     * Given: a deployed file-store service containing a file
     * When: deleting the file
     * Then: the file can no longer be retrieved by id
     */
    @Test
    public void deleteFile() throws IOException, FileStoreServiceConnectorException {
        // Given...
        final File sourceFile = rootFolder.newFile();
        final FileStoreServiceConnector fileStoreServiceConnector =
                new FileStoreServiceConnector(restClient, ITUtil.FILE_STORE_BASE_URL);

        try (final InputStream is = getInputStreamForFile(sourceFile.toPath())) {
            final String fileId = fileStoreServiceConnector.addFile(is);

            // When...
            fileStoreServiceConnector.deleteFile(fileId);

            // Then...
            final HttpClient httpClient = HttpClient.create(HttpClient.newClient());
            final HttpGet httpGet = new HttpGet(httpClient)
                    .withBaseUrl(fileStoreServiceConnector.getBaseUrl())
                    .withPathElements(
                            new PathBuilder(FileStoreServiceConstants.FILE)
                                    .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId)
                                    .build());
            assertThat(httpGet.execute().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    private static void writeFile(Path path, InputStream is) throws IOException {
        try (final OutputStream os = getOutputStreamForFile(path)) {
            final byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = is.read(buf)) > 0) {
                os.write(buf, 0, bytesRead);
            }
            os.flush();
        }
    }

    private static InputStream getInputStreamForFile(Path file) throws IOException {
        return new BufferedInputStream(new FileInputStream(file.toFile()));
    }

    private static OutputStream getOutputStreamForFile(Path file) throws IOException {
        return new BufferedOutputStream(new FileOutputStream(file.toFile()));
    }

    private static Client newRestClient() {
        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, BUFFER_SIZE);
        return HttpClient.newClient(config);
    }

    private static void tearDownRestClient() {
        try {
            if (restClient != null) {
                restClient.close();
            }
        } catch (Exception e) {
        }
    }

    private static void createSparseFile(File destination, long fileSizeInBytes) throws IOException {
        // This creates a sparse file matching maximum available heap size
        // https://en.wikipedia.org/wiki/Sparse_file
        try (RandomAccessFile sparseFile = new RandomAccessFile(destination, "rw")) {
            sparseFile.setLength(fileSizeInBytes);
        }
        assertThat(Files.size(destination.toPath()) > 0, is(true));
    }
}
