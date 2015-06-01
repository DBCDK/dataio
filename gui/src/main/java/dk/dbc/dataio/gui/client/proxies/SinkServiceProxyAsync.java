package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.PingResponseModel;
import dk.dbc.dataio.gui.client.model.SinkModel;

public interface SinkServiceProxyAsync {
    void ping(SinkModel model, AsyncCallback<PingResponseModel> async);
    void close(AsyncCallback<Void> async);
}
