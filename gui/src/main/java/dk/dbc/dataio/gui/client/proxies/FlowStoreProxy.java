package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.engine.Flow;
import dk.dbc.dataio.engine.FlowComponent;
import dk.dbc.dataio.engine.FlowContent;

import java.util.List;

@RemoteServiceRelativePath("FlowStoreProxy")
public interface FlowStoreProxy extends RemoteService {

    void createFlow(FlowContent flowContent) throws NullPointerException, IllegalStateException;

    void addFlowComponentToFlow(Flow flow, FlowComponent flowComponent);

    List<FlowComponent> findAllComponents();

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
