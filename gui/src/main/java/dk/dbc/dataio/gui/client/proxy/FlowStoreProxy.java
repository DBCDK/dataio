package dk.dbc.dataio.gui.client.proxy;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.model.FlowData;

@RemoteServiceRelativePath("FlowStoreProxy")
public interface FlowStoreProxy extends RemoteService {

    public void createFlow(FlowData flowData) throws NullPointerException, IllegalStateException;

    public static class Factory {

        private static FlowStoreProxyAsync asyncInstance = null;

        public static FlowStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(FlowStoreProxy.class);
            }
            return asyncInstance;
        }
    }
}
