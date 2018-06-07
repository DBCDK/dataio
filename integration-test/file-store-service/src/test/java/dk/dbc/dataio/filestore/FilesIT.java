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

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
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
import org.glassfish.jersey.jackson.JacksonFeature;
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
import java.util.List;
import java.util.Objects;

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
    public void fileMetadata() throws FileStoreServiceConnectorException {
        final FileStoreServiceConnector fileStoreServiceConnector =
                new FileStoreServiceConnector(restClient, ITUtil.FILE_STORE_BASE_URL);
        final Metadata barMetadata = new Metadata("bar");
        final String barFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("bar"));
        fileStoreServiceConnector.addMetadata(barFileId, barMetadata);
        final Metadata bazMetadata = new Metadata("baz");
        final String bazFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("baz"));
        fileStoreServiceConnector.addMetadata(bazFileId, bazMetadata);

        final List<ExistingFile> files = fileStoreServiceConnector
                .searchByMetadata(barMetadata, ExistingFile.class);
        assertThat("number of files found", files.size(), is(1));
        assertThat("file id", files.get(0).getId(), is(barFileId));
        assertThat("file metadata", files.get(0).getMetadata(), is(barMetadata));
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
        config.register(new JacksonFeature());
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

    private static class Metadata {
        private final String foo;

        @JsonCreator
        public Metadata(
                @JsonProperty("foo") String foo) {
            this.foo = foo;
        }

        public String getFoo() {
            return foo;
        }

        @Override
        public String toString() {
            return "Metadata{" +
                    "foo='" + foo + '\'' +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            Metadata metadata = (Metadata) o;
            return Objects.equals(foo, metadata.foo);
        }

        @Override
        public int hashCode() {
            return Objects.hash(foo);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    private static class ExistingFile {
        private final String id;
        private final Metadata metadata;

        @JsonCreator
        public ExistingFile(
                @JsonProperty("id") String id,
                @JsonProperty("metadata") Metadata metadata) {
            this.id = id;
            this.metadata = metadata;
        }

        public String getId() {
            return id;
        }

        public Metadata getMetadata() {
            return metadata;
        }
    }
}
