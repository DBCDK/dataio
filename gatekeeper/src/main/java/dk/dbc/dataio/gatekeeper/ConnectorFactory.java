package dk.dbc.dataio.gatekeeper;

import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.registry.JMXMetricRegistry;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.invariant.InvariantUtil;
import jakarta.ws.rs.client.Client;
import org.apache.hc.client5.http.config.ConnectionConfig;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManager;
import org.apache.hc.client5.http.impl.io.PoolingHttpClientConnectionManagerBuilder;
import org.apache.hc.core5.util.Timeout;
import org.glassfish.jersey.apache5.connector.Apache5ClientProperties;
import org.glassfish.jersey.apache5.connector.Apache5ConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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

        final PoolingHttpClientConnectionManager connectionManager =
                PoolingHttpClientConnectionManagerBuilder.create()
                        .setDefaultConnectionConfig(ConnectionConfig.custom()
                                .setConnectTimeout(Timeout.ofSeconds(5))
                                .build())
                        .build();
        final ClientConfig config = new ClientConfig();
        config.register(new JacksonFeature());
        config.property(Apache5ClientProperties.CONNECTION_MANAGER, connectionManager);
        config.property(Apache5ClientProperties.REQUEST_CONFIG, RequestConfig.custom()
                .setResponseTimeout(Timeout.ofMilliseconds(180000))
                .build());
        config.connectorProvider(new Apache5ConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        client = HttpClient.newClient(config);
        fileStoreServiceConnector = new FileStoreServiceConnector(client, UserAgent.forInternalRequests(), fileStoreServiceEndpoint);
        jobStoreServiceConnector = new JobStoreServiceConnector(HttpClient.create(client, UserAgent.forInternalRequests()), jobStoreServiceEndpoint, JMXMetricRegistry.create());
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
