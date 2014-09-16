package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;

import java.util.List;

public interface FlowStoreProxyAsync {
    void createFlow(FlowContent flowContent, AsyncCallback<Flow> callback);
    void createFlowBinder(FlowBinderContent flowBinderContent, AsyncCallback<Void> async);
    void createFlowComponent(FlowComponentContent flowComponentContent, AsyncCallback<FlowComponent> async);
    void createSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void createSink(SinkModel model, AsyncCallback<SinkModel> async);

    void updateSink(SinkModel model, AsyncCallback<SinkModel> async);
    void updateSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void updateFlowComponent(FlowComponentContent flowComponentContent, Long id, Long version, AsyncCallback<FlowComponent> async);
    void refreshFlowComponents(Long id, Long version, AsyncCallback<FlowModel> async);
    void refreshFlowComponentsOld(Long id, Long version, AsyncCallback<Flow> async);
    void updateFlow(FlowModel model, AsyncCallback<FlowModel> async);

    void findAllFlows(AsyncCallback<List<Flow>> async);
    void findAllFlowBinders(AsyncCallback<List<FlowBinder>> async);
    void findAllFlowComponentsOld(AsyncCallback<List<FlowComponent>> async);
    void findAllFlowComponents(AsyncCallback<List<FlowComponentModel>> async);
    void findAllSubmitters(AsyncCallback<List<Submitter>> async);
    void findAllSinks(AsyncCallback<List<Sink>> async);

    void getSink(Long id, AsyncCallback<SinkModel> async);
    void getSubmitter(Long id, AsyncCallback<SubmitterModel> async);
    void getFlowComponent(Long id, AsyncCallback<FlowComponent> async);
    void getFlow(Long id, AsyncCallback<FlowModel> async);

    void close(AsyncCallback<Void> async);
}
