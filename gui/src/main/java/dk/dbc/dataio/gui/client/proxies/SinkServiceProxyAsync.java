package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.exceptions.SinkServiceProxyException;

public interface SinkServiceProxyAsync {
    void ping(SinkContent sinkContent, AsyncCallback<PingResponse> async) throws SinkServiceProxyException;

    void close(AsyncCallback<Void> async);
}
