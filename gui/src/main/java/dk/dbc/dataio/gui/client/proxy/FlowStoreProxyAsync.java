package dk.dbc.dataio.gui.client.proxy;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.FlowData;

public interface FlowStoreProxyAsync {
    public void createFlow(FlowData flowData, AsyncCallback<Void> callback);
}
