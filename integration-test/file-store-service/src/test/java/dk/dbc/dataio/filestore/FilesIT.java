package dk.dbc.dataio.filestore;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.dataio.integrationtest.ITUtil;
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
    private static final byte[] DATA = "8 bytes!".getBytes();

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
     * When: trying to retrieve non-existing file
     * Then: connector call throws FileStoreServiceConnectorUnexpectedStatusCodeException
     * with NOT_FOUND status code
     */
    @Test
    public void fileNotFound() throws FileStoreServiceConnectorException {
        // When...
        final FileStoreServiceConnector fileStoreServiceConnector =
                new FileStoreServiceConnector(restClient, ITUtil.FILE_STORE_BASE_URL);
        try {
            fileStoreServiceConnector.getFile("42");
            fail("No Exception thrown");
        } catch (FileStoreServiceConnectorUnexpectedStatusCodeException e) {
            // Then...
            assertThat(Response.Status.fromStatusCode(e.getStatusCode()), is(Response.Status.NOT_FOUND));
        }
    }

    /**
     * Given: a deployed file-store service
     * When: adding a new file of a size larger than the maximum heap size
     * Then: new file can be retrieved by id
     */
    @Test
    public void fileAddedAndRetrieved() throws Exception {
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

        // This creates a sparse file matching maximum available heap size
        // https://en.wikipedia.org/wiki/Sparse_file
        try (RandomAccessFile sparseFile = new RandomAccessFile(sourceFile, "rw")) {
            sparseFile.setLength(veryLargeFileSizeInBytes);
        }
        assertThat(Files.size(sourceFile.toPath()) > 0, is(true));

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
}
