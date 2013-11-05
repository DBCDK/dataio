package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;

public interface SinkServiceProxyAsync {
    void ping(SinkContent sinkContent, AsyncCallback<PingResponse> async);

    void close(AsyncCallback<Void> async);
}
