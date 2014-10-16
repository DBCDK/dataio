package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.FlowBinderModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;

import java.util.List;

public interface FlowStoreProxyAsync {
    // Flows
    void createFlow(FlowModel model, AsyncCallback<FlowModel> async);
    void updateFlow(FlowModel model, AsyncCallback<FlowModel> async);
    void findAllFlowsOld(AsyncCallback<List<Flow>> async);
    void findAllFlows(AsyncCallback<List<FlowModel>> async);
    void getFlow(Long id, AsyncCallback<FlowModel> async);

    // Flow Components
    void createFlowComponent(FlowComponentContent flowComponentContent, AsyncCallback<FlowComponent> async);
    void updateFlowComponent(FlowComponentContent flowComponentContent, Long id, Long version, AsyncCallback<FlowComponent> async);
    void refreshFlowComponentsOld(Long id, Long version, AsyncCallback<Flow> async);
    void refreshFlowComponents(Long id, Long version, AsyncCallback<FlowModel> async);
    void findAllFlowComponentsOld(AsyncCallback<List<FlowComponent>> async);
    void findAllFlowComponents(AsyncCallback<List<FlowComponentModel>> async);
    void getFlowComponent(Long id, AsyncCallback<FlowComponent> async);

    // Flow Binders
    void createFlowBinder(FlowBinderContent flowBinderContent, AsyncCallback<FlowBinder> async);
    void updateFlowBinder(FlowBinderModel model, AsyncCallback<FlowBinderModel> async);
    void findAllFlowBinders(AsyncCallback<List<FlowBinder>> async);
    void getFlowBinder(long id, AsyncCallback<FlowBinderModel> async);

    // Submitters
    void createSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void updateSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void findAllSubmittersOld(AsyncCallback<List<Submitter>> async);
    void findAllSubmitters(AsyncCallback<List<SubmitterModel>> async);
    void getSubmitter(Long id, AsyncCallback<SubmitterModel> async);

    // Sinks
    void createSink(SinkModel model, AsyncCallback<SinkModel> async);
    void updateSink(SinkModel model, AsyncCallback<SinkModel> async);
    void findAllSinksOld(AsyncCallback<List<Sink>> async);
    void findAllSinks(AsyncCallback<List<SinkModel>> async);
    void getSink(Long id, AsyncCallback<SinkModel> async);

    // Other
    void close(AsyncCallback<Void> async);
}
