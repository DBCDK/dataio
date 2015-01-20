package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

import java.util.List;

public interface FlowStoreProxyAsync {
    // Flows
    void createFlow(FlowModel model, AsyncCallback<FlowModel> async);
    void updateFlow(FlowModel model, AsyncCallback<FlowModel> async);
    void findAllFlows(AsyncCallback<List<FlowModel>> async);
    void getFlow(Long id, AsyncCallback<FlowModel> async);

    // Flow Components
    void createFlowComponent(FlowComponentModel model, AsyncCallback<FlowComponentModel> async);
    void updateFlowComponent(FlowComponentModel model, AsyncCallback<FlowComponentModel> async);
    void refreshFlowComponents(Long id, Long version, AsyncCallback<FlowModel> async);
    void findAllFlowComponents(AsyncCallback<List<FlowComponentModel>> async);
    void getFlowComponent(Long id, AsyncCallback<FlowComponentModel> async);

    // Flow Binders
    void createFlowBinder(FlowBinderModel model, AsyncCallback<FlowBinderModel> async);
    void updateFlowBinder(FlowBinderModel model, AsyncCallback<FlowBinderModel> async);
    void findAllFlowBinders(AsyncCallback<List<FlowBinderModel>> async);
    void getFlowBinder(long id, AsyncCallback<FlowBinderModel> async);

    // Submitters
    void createSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void updateSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);
    void findAllSubmitters(AsyncCallback<List<SubmitterModel>> async);
    void getSubmitter(Long id, AsyncCallback<SubmitterModel> async);

    // Sinks
    void createSink(SinkModel model, AsyncCallback<SinkModel> async);
    void updateSink(SinkModel model, AsyncCallback<SinkModel> async);
    void findAllSinks(AsyncCallback<List<SinkModel>> async);
    void getSink(Long id, AsyncCallback<SinkModel> async);

    // Other
    void close(AsyncCallback<Void> async);
}
