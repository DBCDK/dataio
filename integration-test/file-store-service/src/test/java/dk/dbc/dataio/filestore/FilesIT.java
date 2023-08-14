package dk.dbc.dataio.filestore;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.commons.testcontainers.postgres.DBCPostgreSQLContainer;
import dk.dbc.dataio.commons.testcontainers.Containers;
import dk.dbc.dataio.commons.types.rest.FileStoreServiceConstants;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorUnexpectedStatusCodeException;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.PathBuilder;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.output.Slf4jLogConsumer;
import org.testcontainers.containers.wait.strategy.Wait;

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
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.Random;
import java.util.zip.GZIPOutputStream;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static junitx.framework.FileAssert.assertBinaryEquals;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class FilesIT {
    private static final Logger LOGGER = LoggerFactory.getLogger(FilesIT.class);
    private static final DBCPostgreSQLContainer dbContainer = makeDBContainer();
    @ClassRule
    public static GenericContainer<?> filestoreService = Containers.FILE_STORE.makeContainer()
            .withLogConsumer(new Slf4jLogConsumer(LOGGER))
            .withEnv("JAVA_MAX_HEAP_SIZE", "4G")
            .withEnv("FILESTORE_DB_URL", dbContainer.getPayaraDockerJdbcUrl())
            .withEnv("BFS_ROOT", "/tmp/filestore")
            .withExposedPorts(8080)
            .waitingFor(Wait.forHttp(System.getProperty("filestore.it.service.context") + "/status"))
            .withStartupTimeout(Duration.ofMinutes(5));
    private static final int MiB = 1024 * 1024;
    private static final int BUFFER_SIZE = 8192;
    private static FileStoreServiceConnector fileStoreServiceConnector;
    @Rule
    public TemporaryFolder rootFolder = new TemporaryFolder(new File(System.getProperty("build.dir")));

    private static DBCPostgreSQLContainer makeDBContainer() {
        DBCPostgreSQLContainer container = new DBCPostgreSQLContainer().withReuse(false);
        container.start();
        container.exposeHostPort();
        LOGGER.info("Postgres url is:{}", container.getDockerJdbcUrl());
        return container;
    }

    @BeforeClass
    public static void setupFileStoreServiceConnector() {
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(newRestClient(),
                new RetryPolicy().withMaxRetries(0));

        fileStoreServiceConnector = new FileStoreServiceConnector(failSafeHttpClient,
                "http://" + filestoreService.getContainerIpAddress() +
                        ":" + filestoreService.getMappedPort(8080) +
                        System.getProperty("filestore.it.service.context"));
    }

    @AfterClass
    public static void teardown() {
        closeRestClient();
    }

    private static void writeFile(File path, InputStream is) throws IOException {
        try (OutputStream os = getOutputStreamForFile(path.toPath())) {
            byte[] buf = new byte[BUFFER_SIZE];
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
        ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, BUFFER_SIZE);
        config.register(new JacksonFeature());
        return HttpClient.newClient(config);
    }

    private static void closeRestClient() {
        try {
            if (fileStoreServiceConnector != null
                    && fileStoreServiceConnector.getClient() != null) {
                fileStoreServiceConnector.getClient().close();
            }
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static void createSparseFile(File destination, long fileSizeInBytes) throws IOException {
        // https://en.wikipedia.org/wiki/Sparse_file
        try (RandomAccessFile sparseFile = new RandomAccessFile(destination, "rw")) {
            sparseFile.setLength(fileSizeInBytes);
        }
        assertThat(Files.size(destination.toPath()) > 0, is(true));
    }

    private static Connection connectToFileStoreDB() throws SQLException {
        return dbContainer.datasource().getConnection();
    }

    private static void pushBackCreationTime(String fileId) {
        try (Connection conn = connectToFileStoreDB()) {
            PreparedStatement statement = conn.prepareStatement(
                    "UPDATE file_attributes SET creationtime=now()-INTERVAL'7 months' WHERE id=?");
            statement.setInt(1, Integer.parseInt(fileId));
            statement.executeUpdate();
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    private static Date getAtime(String fileId) {
        try (Connection conn = connectToFileStoreDB()) {
            PreparedStatement statement = conn.prepareStatement("SELECT atime FROM file_attributes WHERE id=?");
            statement.setInt(1, Integer.parseInt(fileId));
            ResultSet resultSet = statement.executeQuery();
            if (resultSet.next()) {
                return resultSet.getTimestamp("atime");
            }
            throw new IllegalStateException("file ID " + fileId + " not found");
        } catch (SQLException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Given: a deployed file-store service
     * When: adding a new file
     * Then: new file can be retrieved by id
     */
    @Test
    public void fileAddedAndRetrieved() throws IOException, FileStoreServiceConnectorException {
        // When...

        // On new JVMs and modern machines this is far from
        // exceeding the heap. To do that you should probably
        // add veryLargeFileSizeInBytes += Runtime.getRuntime().maxMemory()
        // but be advised that this requires a lot of free space.
        final long veryLargeFileSizeInBytes = 1024 * MiB; // 1 GiB
        File sourceFile = rootFolder.newFile();
        if (sourceFile.getUsableSpace() < veryLargeFileSizeInBytes * 3) {
            // We need enough space for
            //  1. source file
            //  2. file when uploaded to file store
            //  3. file when read back from file store
            fail("Not enough free space for test: " + (veryLargeFileSizeInBytes * 3) / MiB + " MiB needed");
        }
        createSparseFile(sourceFile, veryLargeFileSizeInBytes);

        try (InputStream is = getInputStreamForFile(sourceFile.toPath())) {
            String fileId = fileStoreServiceConnector.addFile(is);

            // Then...
            InputStream fileStream = fileStoreServiceConnector.getFile(fileId);
            File destinationFile = rootFolder.newFile();
            writeFile(destinationFile, fileStream);
            assertBinaryEquals(sourceFile, destinationFile);
        }
    }

    @Test
    public void fileMetadata() throws FileStoreServiceConnectorException {
        Metadata barMetadata = new Metadata("bar");
        String barFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("bar"));
        fileStoreServiceConnector.addMetadata(barFileId, barMetadata);
        Metadata bazMetadata = new Metadata("baz");
        String bazFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("baz"));
        fileStoreServiceConnector.addMetadata(bazFileId, bazMetadata);

        List<ExistingFile> files = fileStoreServiceConnector
                .searchByMetadata(barMetadata, ExistingFile.class);
        assertThat("number of files found", files.size(), is(1));
        assertThat("file id", files.get(0).getId(), is(barFileId));
        assertThat("file metadata", files.get(0).getMetadata(), is(barMetadata));
    }

    @Test
    public void filesOfTypeMarcconvAreDeletedAfterThreeMonths() throws FileStoreServiceConnectorException {
        // Given: Three files in filestore.
        //      * One is of type marcconv and is older than three months.
        //      * One is of type marcconv. But recent.
        //      * One is of another type.

        Metadata marcconvMetadata = new Metadata("dataio/sink/marcconv");
        String marcconvFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("marcconv sink output data"));
        String recentMarcconvFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("marcconv sink output data. More recent."));
        fileStoreServiceConnector.addMetadata(marcconvFileId, marcconvMetadata);
        fileStoreServiceConnector.addMetadata(recentMarcconvFileId, marcconvMetadata);
        Metadata bazMetadata = new Metadata("baz");
        String bazFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("baz"));
        fileStoreServiceConnector.addMetadata(bazFileId, bazMetadata);

        pushBackCreationTime(marcconvFileId);

        // When a purge is run
        fileStoreServiceConnector.purge();

        // Then the file of type marcconv which is more than three months old is no longer present.
        //   The rest is left untouched.
        InputStream bazContent = fileStoreServiceConnector
                .getFile(bazFileId);
        InputStream recentMarcconvContent = fileStoreServiceConnector
                .getFile(recentMarcconvFileId);
        assertThat("recent marcconv file is still there", StringUtil.asString(recentMarcconvContent), is("marcconv sink output data. More recent."));
        assertThat("baz file still there", StringUtil.asString(bazContent), is("baz"));
        assertThat(() -> fileStoreServiceConnector.getFile(marcconvFileId), isThrowing(FileStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    @Test
    public void appendToFile() throws FileStoreServiceConnectorException {
        String fileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("1"));
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

        InputStream inputStream = new ByteArrayInputStream(data);
        String fileId = fileStoreServiceConnector.addFile(inputStream);

        // Then...
        long byteSize = fileStoreServiceConnector.getByteSize(fileId);
        assertThat(byteSize, is((long) data.length));
    }

    /**
     * Given: a deployed file-store service containing a file
     * When: deleting the file
     * Then: the file can no longer be retrieved by id
     */
    @Test
    public void deleteFile() throws IOException, FileStoreServiceConnectorException {
        // Given...
        File sourceFile = rootFolder.newFile();

        try (InputStream is = getInputStreamForFile(sourceFile.toPath())) {
            String fileId = fileStoreServiceConnector.addFile(is);

            // When...
            fileStoreServiceConnector.deleteFile(fileId);

            // Then...
            HttpClient httpClient = HttpClient.create(HttpClient.newClient());
            HttpGet httpGet = new HttpGet(httpClient)
                    .withBaseUrl(fileStoreServiceConnector.getBaseUrl())
                    .withPathElements(
                            new PathBuilder(FileStoreServiceConstants.FILE)
                                    .bind(FileStoreServiceConstants.FILE_ID_VARIABLE, fileId)
                                    .build());
            assertThat(httpGet.execute().getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
        }
    }

    /**
     * Given: a deployed file-store service containing two files
     * When: deleting one file
     * Then: the file can no longer be retrieved by id
     * But: the other file can still be fetched.
     */
    @Test(timeout = 30000)
    public void checkForDeadlockAfterGetfileWithNonExistantFile() throws IOException, FileStoreServiceConnectorException {
        // Given
        //   Two files in filestore
        String fileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("a file"));
        String anotherFileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("another file"));


        // When first one is deleted
        fileStoreServiceConnector.deleteFile(fileId);

        // Then, when we try to fetch the first file, and tries to fetch another arbitrary also nonexistant file
        //      through the fileStoreServiceConnector, FileStoreServiceConnectorUnexpectedStatusCodeExceptions
        //      are raised. (Caused by http code 404, not found).
        //
        // But subsequent calls to getFile do not lock. (Exsistant as well as non-existant).
        assertThat(() -> fileStoreServiceConnector.getFile(fileId), isThrowing(FileStoreServiceConnectorUnexpectedStatusCodeException.class));
        assertThat("getfile does not deadlock", StringUtil.asString(fileStoreServiceConnector.getFile(anotherFileId)), is("another file"));
        assertThat(() -> fileStoreServiceConnector.getFile("99999999"), isThrowing(FileStoreServiceConnectorUnexpectedStatusCodeException.class));
    }

    /**
     * Given: a deployed file-store service
     * When: adding a gzip'ed fil
     * Then: the file content can be retrieved in its decompressed form
     * And: the file byte size is reported for its decompressed form
     */
    @Test
    public void gzipDefaultHandling() throws IOException, FileStoreServiceConnectorException {
        // When...
        File sourceFile = createFile(getRandomBytes(512));
        File gzFile = createGzFile(sourceFile);

        try (InputStream is = getInputStreamForFile(gzFile.toPath())) {
            String fileId = fileStoreServiceConnector.addFile(is);

            // Then...
            InputStream fileStream = fileStoreServiceConnector.getFile(fileId);
            File destinationFile = rootFolder.newFile();
            writeFile(destinationFile, fileStream);
            assertBinaryEquals("file content", sourceFile, destinationFile);
            // And,..
            assertThat("file size", fileStoreServiceConnector.getByteSize(fileId),
                    is(sourceFile.length()));
        }
    }

    /**
     * Given: a newly created file
     * When: the file has not been read
     * Then: atime is null
     * When: the file is read
     * Then: atime is set
     */
    @Test
    public void atime() throws FileStoreServiceConnectorException, IOException {
        String fileId = fileStoreServiceConnector.addFile(StringUtil.asInputStream("a file"));
        assertThat("atime before read", getAtime(fileId), is(nullValue()));

        StringUtil.asString(fileStoreServiceConnector.getFile(fileId));

        Date atime = getAtime(fileId);
        assertThat("atime after read", atime, is(notNullValue()));
        assertThat("atime is recent", atime.toInstant().isAfter(Instant.now().minusSeconds(5)), is(true));

        StringUtil.asString(fileStoreServiceConnector.getFile(fileId));
        assertThat("atime updated on subsequent reads", getAtime(fileId).toInstant().isAfter(atime.toInstant()), is(true));
    }

    @Test
    public void purgingOfFilesNeverRead() throws FileStoreServiceConnectorException {
        String oldAndNeverRead = fileStoreServiceConnector.addFile(StringUtil.asInputStream("old - never read"));
        String oldAndRead = fileStoreServiceConnector.addFile(StringUtil.asInputStream("old - read"));
        String newAndNeverRead = fileStoreServiceConnector.addFile(StringUtil.asInputStream("new - never read"));

        pushBackCreationTime(oldAndNeverRead);
        pushBackCreationTime(oldAndRead);
        StringUtil.asString(fileStoreServiceConnector.getFile(oldAndRead));

        fileStoreServiceConnector.purge();

        assertThat("oldAndNeverRead removed by purge", () -> fileStoreServiceConnector.getFile(oldAndNeverRead),
                isThrowing(FileStoreServiceConnectorUnexpectedStatusCodeException.class));
        assertThat("oldAndRead remains after purge",
                StringUtil.asString(fileStoreServiceConnector.getFile(oldAndRead)), is("old - read"));
        assertThat("newAndNeverRead remains after purge",
                StringUtil.asString(fileStoreServiceConnector.getFile(newAndNeverRead)), is("new - never read"));
    }

    private File createFile(byte[] bytes) throws IOException {
        File file = rootFolder.newFile();
        Files.write(file.toPath(), bytes);
        return file;
    }

    private File createGzFile(File sourceFile) throws IOException {
        File gzFile = new File(sourceFile.getAbsolutePath() + ".gz");
        try (FileInputStream in = new FileInputStream(sourceFile);
             GZIPOutputStream gzOut = new GZIPOutputStream(
                     new FileOutputStream(gzFile))) {

            byte[] buf = new byte[BUFFER_SIZE];
            int bytesRead;
            while ((bytesRead = in.read(buf)) > 0) {
                gzOut.write(buf, 0, bytesRead);
            }
        }
        return gzFile;
    }

    private byte[] getRandomBytes(int numBytes) {
        byte[] bytes = new byte[numBytes];
        new Random().nextBytes(bytes);
        return bytes;
    }

    private static class Metadata {
        private final String origin;

        @JsonCreator
        public Metadata(
                @JsonProperty("origin") String origin) {
            this.origin = origin;
        }

        public String getOrigin() {
            return origin;
        }

        @Override
        public String toString() {
            return "Metadata{" +
                    "origin='" + origin + '\'' +
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
            return Objects.equals(origin, metadata.origin);
        }

        @Override
        public int hashCode() {
            return Objects.hash(origin);
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
