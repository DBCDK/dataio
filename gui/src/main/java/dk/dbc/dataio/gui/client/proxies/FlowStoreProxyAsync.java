package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import java.util.List;

public interface FlowStoreProxyAsync {
    void createFlow(FlowContent flowContent, AsyncCallback<Void> callback);
    void createFlowBinder(FlowBinderContent flowBinderContent, AsyncCallback<Void> async);
    void createFlowComponent(FlowComponentContent flowComponentContent, AsyncCallback<Void> async);
    void createSubmitter(SubmitterContent submitterContent, AsyncCallback<Void> async);
    void createSink(SinkContent sinkContent, AsyncCallback<Void> async);

    void findAllFlows(AsyncCallback<List<Flow>> async);
    void findAllFlowBinders(AsyncCallback<List<FlowBinder>> async);
    void findAllComponents(AsyncCallback<List<FlowComponent>> async);
    void findAllSubmitters(AsyncCallback<List<Submitter>> async);
    void findAllSinks(AsyncCallback<List<Sink>> async);

    void close(AsyncCallback<Void> async);
}
