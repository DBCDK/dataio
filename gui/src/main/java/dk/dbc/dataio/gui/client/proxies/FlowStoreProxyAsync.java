package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.engine.Flow;
import dk.dbc.dataio.engine.FlowComponent;
import dk.dbc.dataio.engine.FlowContent;

import java.util.List;

public interface FlowStoreProxyAsync {
    void createFlow(FlowContent flowContent, AsyncCallback<Void> callback);

    void findAllComponents(AsyncCallback<List<FlowComponent>> async);

    void findAllFlows(AsyncCallback<List<Flow>> async);
    
    void addFlowComponentToFlow(Flow flow, FlowComponent flowComponent, AsyncCallback<Void> async);
}
