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
import dk.dbc.dataio.gui.client.pages.submittermodify.Model;

import java.util.List;

public interface FlowStoreProxyAsync {
    void createFlow(FlowContent flowContent, AsyncCallback<Flow> callback);
    void createFlowBinder(FlowBinderContent flowBinderContent, AsyncCallback<Void> async);
    void createFlowComponent(FlowComponentContent flowComponentContent, AsyncCallback<FlowComponent> async);
    void createSubmitter(SubmitterContent submitterContent, AsyncCallback<Submitter> async);
    void createSink(SinkContent sinkContent, AsyncCallback<Sink> async);

    void updateSink(SinkContent sinkContent, Long id, Long version, AsyncCallback<Sink> async);
    void updateSubmitter(Model model, Long id, Long version, AsyncCallback<Model> async);
    void updateFlowComponent(FlowComponentContent flowComponentContent, Long id, Long version, AsyncCallback<FlowComponent> async);
    void updateFlowComponentsInFlowToLatestVersion(Long id, Long version, AsyncCallback<Flow> async);

    void findAllFlows(AsyncCallback<List<Flow>> async);
    void findAllFlowBinders(AsyncCallback<List<FlowBinder>> async);
    void findAllFlowComponents(AsyncCallback<List<FlowComponent>> async);
    void findAllSubmitters(AsyncCallback<List<Submitter>> async);
    void findAllSinks(AsyncCallback<List<Sink>> async);

    void getSink(Long id, AsyncCallback<Sink> async);
    void getSubmitter(Long id, AsyncCallback<Model> async);
    void getFlowComponent(Long id, AsyncCallback<FlowComponent> async);

    void close(AsyncCallback<Void> async);
}
