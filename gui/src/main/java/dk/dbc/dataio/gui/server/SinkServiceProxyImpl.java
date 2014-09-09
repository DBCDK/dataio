package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.rest.SinkServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.commons.utils.jersey.jackson.Jackson2xFeature;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.proxies.SinkServiceProxy;
import dk.dbc.dataio.gui.server.ModelMappers.SinkModelMapper;
import org.glassfish.jersey.client.ClientConfig;

import javax.servlet.ServletException;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;

public class SinkServiceProxyImpl implements SinkServiceProxy {
    private Client client = null;

    public SinkServiceProxyImpl() {
        final ClientConfig clientConfig = new ClientConfig().register(new Jackson2xFeature());
        client = HttpClient.newClient(clientConfig);
    }

    @Override
    public PingResponse ping(SinkModel model) throws ProxyException {
        InvariantUtil.checkNotNullOrThrow(model, "model");

        final Response response;
        final PingResponse result;
        try {
            response = HttpClient.doPostWithJson(client, SinkModelMapper.toSinkContent(model),
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
