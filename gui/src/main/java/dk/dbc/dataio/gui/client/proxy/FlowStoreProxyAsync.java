package dk.dbc.dataio.gui.client.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.Flow;

public interface FlowStoreProxyAsync {
    public void createFlow(Flow flow, AsyncCallback<Void> callback);
}
