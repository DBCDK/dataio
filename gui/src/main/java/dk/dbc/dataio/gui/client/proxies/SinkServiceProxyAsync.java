package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.gui.client.pages.sinkmodify.SinkModel;

public interface SinkServiceProxyAsync {
    void ping(SinkContent sinkContent, AsyncCallback<PingResponse> async);
    void ping(SinkModel model, AsyncCallback<PingResponse> async);
    void close(AsyncCallback<Void> async);
}
