package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.sinkmodify.SinkModel;

@RemoteServiceRelativePath("SinkServiceProxy")
public interface SinkServiceProxy extends RemoteService {
    PingResponse ping(SinkModel model) throws ProxyException;

    void close();

    class Factory {
        private static SinkServiceProxyAsync asyncInstance = null;

        public static SinkServiceProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(SinkServiceProxy.class);
            }
            return asyncInstance;
        }
    }
}
