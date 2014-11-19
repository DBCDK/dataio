package dk.dbc.dataio.filestore.service.connector;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.LinkedList;
import java.util.Queue;

/**
 * Mocked FileStoreServiceConnector implementation able to intercept
 * calls to addFile() writing file content to local destinations instead
 */
public class MockedFileStoreServiceConnector extends FileStoreServiceConnector {
    public static final String FILE_ID = "42";
    public final Queue<Path> destinations;

    public MockedFileStoreServiceConnector() {
        super(HttpClient.newClient(), "baseurl");
        this.destinations = new LinkedList<>();
    }

    @Override
    public String addFile(InputStream inputStream) {
        final Path destination = destinations.remove();
        try (final FileOutputStream fos = new FileOutputStream(destination.toFile())) {
            final byte[] buf = new byte[8192];
            int bytesRead;
            while ((bytesRead = inputStream.read(buf)) > 0) {
                fos.write(buf, 0, bytesRead);
            }
            fos.flush();
        } catch (IOException e) {
            throw new IllegalStateException("Unable to write file " + destination, e);
        }
        return FILE_ID;
    }
}
