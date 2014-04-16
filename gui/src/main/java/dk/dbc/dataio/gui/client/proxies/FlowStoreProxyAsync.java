package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.*;

import java.util.List;

public interface FlowStoreProxyAsync {
    void createFlow(FlowContent flowContent, AsyncCallback<Void> callback);
    void createFlowBinder(FlowBinderContent flowBinderContent, AsyncCallback<Void> async);
    void createFlowComponent(FlowComponentContent flowComponentContent, AsyncCallback<Void> async);
    void createSubmitter(SubmitterContent submitterContent, AsyncCallback<Void> async);
    void createSink(SinkContent sinkContent, AsyncCallback<Void> async);

    void updateSink(Sink sink, Long id, Long version, AsyncCallback<Void> async);

    void findAllFlows(AsyncCallback<List<Flow>> async);
    void findAllFlowBinders(AsyncCallback<List<FlowBinder>> async);
    void findAllComponents(AsyncCallback<List<FlowComponent>> async);
    void findAllSubmitters(AsyncCallback<List<Submitter>> async);
    void findAllSinks(AsyncCallback<List<Sink>> async);

    void getSink(Long id, AsyncCallback<Sink> async);

    void close(AsyncCallback<Void> async);
}
