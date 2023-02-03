package dk.dbc.dataio.gatekeeper;

import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.invariant.InvariantUtil;
import org.apache.http.client.config.RequestConfig;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.client.Client;

public class ConnectorFactory {
    private static final Logger LOGGER = LoggerFactory.getLogger(ConnectorFactory.class);
    private final Client client;
    private final FileStoreServiceConnector fileStoreServiceConnector;
    private final JobStoreServiceConnector jobStoreServiceConnector;

    public ConnectorFactory(String fileStoreServiceEndpoint, String jobStoreServiceEndpoint)
            throws NullPointerException, IllegalArgumentException {
        InvariantUtil.checkNotNullNotEmptyOrThrow(fileStoreServiceEndpoint, "fileStoreServiceEndpoint");
        InvariantUtil.checkNotNullNotEmptyOrThrow(jobStoreServiceEndpoint, "jobStoreServiceEndpoint");

        LOGGER.info("fileStoreServiceEndpoint: {}", fileStoreServiceEndpoint);
        LOGGER.info("jobStoreServiceEndpoint: {}", jobStoreServiceEndpoint);

        final ClientConfig config = new ClientConfig();
        config.register(new JacksonFeature());
        config.property(ApacheClientProperties.REQUEST_CONFIG, RequestConfig.custom()
                .setConnectTimeout(5000)
                .setSocketTimeout(180000)
                .build());
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        client = HttpClient.newClient(config);
        fileStoreServiceConnector = new FileStoreServiceConnector(client, fileStoreServiceEndpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(client, jobStoreServiceEndpoint, null);
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
