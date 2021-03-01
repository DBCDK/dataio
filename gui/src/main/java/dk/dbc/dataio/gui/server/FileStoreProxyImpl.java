package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnectorException;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FileStoreProxy;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;
import java.io.InputStream;

public class FileStoreProxyImpl implements FileStoreProxy {
    final Client client;
    private final String baseUrl;
    FileStoreServiceConnector fileStoreServiceConnector;

    public FileStoreProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("FILESTORE_URL");
        fileStoreServiceConnector = new FileStoreServiceConnector(client, baseUrl);
    }

    @Override
    public void removeFile(String fileId) throws ProxyException {
        try {
            fileStoreServiceConnector.deleteFile(fileId);
        } catch (Exception e) {
            throw new ProxyException(ProxyError.INTERNAL_SERVER_ERROR, e.getCause());
        }
    }

    @Override
    public void close() {
        HttpClient.closeClient(client);
    }

    public String addFile(InputStream dataSource) throws ProxyException {
        try {
            return fileStoreServiceConnector.addFile(dataSource);
        } catch (FileStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.INTERNAL_SERVER_ERROR, e.getCause());
        }
    }
}
