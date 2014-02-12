package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.rest.SinkServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;

public class SinkServiceProxyImpl implements SinkServiceProxy {
    private Client client = null;

    public SinkServiceProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new JacksonFeature());
        client = HttpClient.newClient(clientConfig);
    }

    @Override
    public PingResponse ping(SinkContent sinkContent) throws ProxyException {
        InvariantUtil.checkNotNullOrThrow(sinkContent, "sinkContent");

        final Response response;
        final PingResponse result;
        try {
            response = HttpClient.doPostWithJson(client, sinkContent,
                    ServletUtil.getSinkServiceEndpoint(), SinkServiceConstants.PING);
        } catch (ServletException e) {
            throw new ProxyException(ProxyError.SERVICE_NOT_FOUND, e);
        }
        try {
            assertStatusCode(response, Response.Status.OK);
            result = response.readEntity(PingResponse.class);
        } finally {
            response.close();
        }
        return result;
    }

    @Override
    public void close() {
        HttpClient.closeClient(client);
    }

    private void assertStatusCode(Response response, Response.Status expectedStatus) throws ProxyException {
        final Response.Status status = Response.Status.fromStatusCode(response.getStatus());
        if (status != expectedStatus) {
            final ProxyError errorCode;
            switch (status) {
                case BAD_REQUEST: errorCode = ProxyError.BAD_REQUEST;
                    break;
                case NOT_FOUND: errorCode = ProxyError.SERVICE_NOT_FOUND;
                    break;
                default:
                    errorCode = ProxyError.INTERNAL_SERVER_ERROR;
            }
            throw new ProxyException(errorCode, response.readEntity(String.class));
        }
    }
}
