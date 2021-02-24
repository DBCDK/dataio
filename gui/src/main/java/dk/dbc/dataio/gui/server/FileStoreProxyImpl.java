package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FileStoreProxy;
import dk.dbc.httpclient.HttpClient;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

import javax.ws.rs.client.Client;

public class FileStoreProxyImpl implements FileStoreProxy {
    final Client client;
    private final String baseUrl;

    public FileStoreProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
        baseUrl = ServiceUtil.getStringValueFromSystemEnvironmentOrProperty("FILESTORE_URL");
    }

    @Override
    public void removeFile(String fileId) throws ProxyException {

    }

    @Override
    public void close() {
    }

    @Override
    public String addFile(byte[] dataSource) throws ProxyException {
        return "42";
    }
}
