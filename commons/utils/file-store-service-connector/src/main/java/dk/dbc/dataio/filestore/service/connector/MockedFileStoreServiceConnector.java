package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.httpclient.HttpClient;

import javax.ws.rs.ProcessingException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Mocked FileStoreServiceConnector implementation intercepting calls ensuring data is stored locally.
 * <p>
 * This class is not thread-safe.
 */
public class MockedFileStoreServiceConnector extends FileStoreServiceConnector {
    public static final String BASEURL = "baseurl";

    public final Queue<Path> destinations;
    public Path currentDestination;
    public Object metadata;

    private int sequenceNumber = 0;

    public MockedFileStoreServiceConnector() {
        super(HttpClient.newClient(), BASEURL);
        this.destinations = new LinkedList<>();
    }

    /**
     * Uses head entry from public destinations field of this object as local file destination
     * and writes file content
     *
     * @param inputStream stream of bytes to be written
     * @return hardcoded file ID
     */
    @Override
    public String addFile(InputStream inputStream) {
        currentDestination = destinations.remove();
        try (FileOutputStream fos = new FileOutputStream(currentDestination.toFile())) {
            final byte[] buf = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                fos.write(buf, 0, bytesRead);
            }
            fos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file " + currentDestination, e);
        }

        return String.valueOf(++sequenceNumber);
    }

    /**
     * Appends bytes to local file used by last call of addFile
     *
     * @param fileId ID of existing file to append to
     * @param bytes  bytes to be appended
     */
    @Override
    public void appendToFile(String fileId, byte[] bytes) {
        assertFileId(fileId);
        try (FileOutputStream fos = new FileOutputStream(currentDestination.toFile(), true)) {
            fos.write(bytes);
            fos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to append to file " + currentDestination, e);
        }
    }

    @Override
    public InputStream getFile(final String fileId)
            throws NullPointerException, IllegalArgumentException, ProcessingException, FileStoreServiceConnectorException {
        assertFileId(fileId);
        try {
            File initialFile = currentDestination.toFile();
            return new FileInputStream(initialFile);
        } catch (FileNotFoundException e) {
            throw new ProcessingException("File not found");
        }
    }

    /**
     * Sets metadata object in public metadata field of this object
     *
     * @param fileId   ID of existing file
     * @param metadata metadata to be added
     */
    @Override
    public void addMetadata(String fileId, Object metadata) {
        assertFileId(fileId);
        this.metadata = metadata;
    }

    /**
     * Noop deletion of file
     *
     * @param fileId ID of file to be deleted
     */
    @Override
    public void deleteFile(String fileId) {
        assertFileId(fileId);
    }

    public String getCurrentFileId() {
        return String.valueOf(sequenceNumber);
    }

    private void assertFileId(String fileId) {
        if (!String.valueOf(sequenceNumber).equals(fileId)) {
            throw new IllegalArgumentException("File ID was " + fileId + " must be " + sequenceNumber);
        }
    }
}
