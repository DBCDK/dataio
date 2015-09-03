package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;

import javax.ws.rs.client.Client;

public class ConnectorFactory {
    private final Client client;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    public ConnectorFactory(String fileStoreServiceEndpoint, String jobStoreServiceEndpoint)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreServiceEndpoint, "fileStoreServiceEndpoint");
        InvariantUtil.checkNotNullNotEmptyOrThrow(jobStoreServiceEndpoint, "jobStoreServiceEndpoint");

        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        client = HttpClient.newClient(config);
        fileStoreServiceConnector = new FileStoreServiceConnector(client, fileStoreServiceEndpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(client, fileStoreServiceEndpoint);
    }

    public FileStoreServiceConnector getFileStoreServiceConnector() {
        return fileStoreServiceConnector;
    }

    public JobStoreServiceConnector getJobStoreServiceConnector() {
        return jobStoreServiceConnector;
    }

    public void close() {
        HttpClient.closeClient(client);
    }
}
