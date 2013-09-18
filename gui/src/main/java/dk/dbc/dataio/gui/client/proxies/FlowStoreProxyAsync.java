package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.engine.Flow;
import dk.dbc.dataio.engine.FlowBinderContent;
import dk.dbc.dataio.engine.FlowComponent;
import dk.dbc.dataio.engine.FlowComponentContent;
import dk.dbc.dataio.engine.FlowContent;
import dk.dbc.dataio.engine.Submitter;
import dk.dbc.dataio.engine.SubmitterContent;

import java.util.List;

public interface FlowStoreProxyAsync {
    void createFlow(FlowContent flowContent, AsyncCallback<Void> callback);
    void createFlowBinder(FlowBinderContent flowBinderContent, AsyncCallback<Void> async);
    void createFlowComponent(FlowComponentContent flowComponentContent, AsyncCallback<Void> async);
    void createSubmitter(SubmitterContent submitterContent, AsyncCallback<Void> async);

    void findAllComponents(AsyncCallback<List<FlowComponent>> async);
    void findAllFlows(AsyncCallback<List<Flow>> async);
    void findAllSubmitters(AsyncCallback<List<Submitter>> async);

}
