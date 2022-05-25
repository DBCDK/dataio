package dk.dbc.dataio.cli;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.httpclient.HttpClient;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collection;

public class FileStoreFetcher {
    private static final long  MEGABYTE = 1024L * 1024L;

    private final FileStoreServiceConnector fileStoreServiceConnector;

    public FileStoreFetcher(String fileStoreServiceEndpoint) {
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(1);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(1);

        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        config.property(ApacheClientProperties.CONNECTION_MANAGER, poolingHttpClientConnectionManager);
        config.register(new JacksonFeature());
        final Client client = HttpClient.newClient(config);
        fileStoreServiceConnector = new FileStoreServiceConnector(client, fileStoreServiceEndpoint);
    }

    public long getDownloadSizeMB(Collection<Datafile> datafiles) {
        long downloadSizeBytes = 0;
        try {
            for (Datafile datafile : datafiles) {
                downloadSizeBytes += fileStoreServiceConnector.getByteSize(datafile.getFileId());
            }
        } catch (FileStoreServiceConnectorException e) {
            throw new CliException("Unable to get size for file", e);
        }
        return downloadSizeBytes / MEGABYTE;
    }

    public void downloadFile(Datafile datafile, Path destinationDir) {
        if (Files.exists(destinationDir)) {
            try {
                Files.createDirectories(destinationDir);
            } catch (IOException e) {
                throw new CliException("Unable to create destination dir", e);
            }
        }
        try (InputStream in = new BufferedInputStream(fileStoreServiceConnector.getFile(datafile.getFileId()))) {
            Files.copy(in, destinationDir.resolve(datafile.getFileId()),
                    StandardCopyOption.REPLACE_EXISTING);
        } catch (FileStoreServiceConnectorException e) {
            throw new CliException("Unable to read file " + datafile.getFileId(), e);
        } catch (IOException e) {
            throw new CliException("Unable to write file " + datafile.getFileId(), e);
        }
    }
}
