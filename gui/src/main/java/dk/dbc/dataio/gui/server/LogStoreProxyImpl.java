package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.LogStoreProxy;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorException;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorUnexpectedStatusCodeException;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class LogStoreProxyImpl implements LogStoreProxy {
    final Client client;
    final String baseUrl;
    LogStoreServiceConnector logStoreServiceConnector;

    public LogStoreProxyImpl() throws NamingException {
        client = HttpClient.newClient();
        baseUrl = ServiceUtil.getLogStoreServiceEndpoint();
        logStoreServiceConnector = new LogStoreServiceConnector(client, baseUrl);
    }

    //This constructor is intended for test purpose only with reference to dependency injection.
    LogStoreProxyImpl(LogStoreServiceConnector logStoreServiceConnector) throws NamingException{
        this.logStoreServiceConnector = logStoreServiceConnector;
        client = HttpClient.newClient();
        baseUrl = ServiceUtil.getLogStoreServiceEndpoint();
    }

    @Override
    public String getItemLog(String jobId, Long chunkId, Long itemId) throws ProxyException {
        final String itemLog;
        try {
            itemLog = logStoreServiceConnector.getItemLog(jobId, chunkId, itemId);
        } catch (NullPointerException e) {
            throw new ProxyException(ProxyError.BAD_REQUEST, e);
        } catch (IllegalArgumentException e) {
            throw new ProxyException(ProxyError.BAD_REQUEST, e);
        } catch (LogStoreServiceConnectorUnexpectedStatusCodeException e) {
            throw new ProxyException(translateToProxyError(e.getStatusCode()),e.getMessage());
        } catch (LogStoreServiceConnectorException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        return itemLog;
    }

    private ProxyError translateToProxyError(int statusCode)throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(statusCode);

        final ProxyError errorCode;
        switch (status){
            case NOT_FOUND: errorCode = ProxyError.ENTITY_NOT_FOUND;
                break;
            default:
                errorCode = ProxyError.INTERNAL_SERVER_ERROR;
        }
        return errorCode;
    }

    public void close() {
        HttpClient.closeClient(client);
    }
}
