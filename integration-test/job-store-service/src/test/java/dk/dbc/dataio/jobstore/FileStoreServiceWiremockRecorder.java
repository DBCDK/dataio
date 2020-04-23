/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.jobstore;

import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.httpclient.HttpClient;
import org.apache.commons.compress.utils.IOUtils;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.glassfish.jersey.apache.connector.ApacheClientProperties;
import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.io.IOException;
import java.io.InputStream;

public class FileStoreServiceWiremockRecorder {
    private final FileStoreServiceConnector fileStoreServiceConnector;

    /*
            Steps to reproduce wiremock recording:

            - Start standalone runner
                java -jar wiremock-[WIRE_MOCK_VERSION]-standalone.jar --proxy-all="[FILE_STORE_SERVICE_URL]" --record-mappings --verbose

            - Run the main method of this class

            - Replace content of src/test/resources/{__files|mappings} with that produced by the standalone runner
         */
    public static void main(String[] args) throws FileStoreServiceConnectorException, IOException {
        final FileStoreServiceWiremockRecorder recorder = new FileStoreServiceWiremockRecorder();
        recorder.getFile("13613666"); // TRANSIENT file, so this probably no longer exists when re-recording
    }

    private FileStoreServiceWiremockRecorder() {
        final PoolingHttpClientConnectionManager poolingHttpClientConnectionManager =
                new PoolingHttpClientConnectionManager();
        poolingHttpClientConnectionManager.setMaxTotal(10);
        poolingHttpClientConnectionManager.setDefaultMaxPerRoute(10);

        final ClientConfig config = new ClientConfig();
        config.connectorProvider(new ApacheConnectorProvider());
        config.property(ClientProperties.CHUNKED_ENCODING_SIZE, 8 * 1024);
        config.property(ApacheClientProperties.CONNECTION_MANAGER, poolingHttpClientConnectionManager);
        config.register(new JacksonFeature());
        Client client = HttpClient.newClient(config);
        fileStoreServiceConnector = new FileStoreServiceConnector(client, "http://localhost:8080");
    }

    private void getFile(String fileId) throws FileStoreServiceConnectorException, IOException {
        final InputStream file = fileStoreServiceConnector.getFile(fileId);
        IOUtils.toByteArray(file);
        fileStoreServiceConnector.getByteSize(fileId);
    }
}
