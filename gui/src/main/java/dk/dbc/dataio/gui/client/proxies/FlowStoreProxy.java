package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;

import java.util.List;

@RemoteServiceRelativePath("FlowStoreProxy")
public interface FlowStoreProxy extends RemoteService {
    // Flows
    FlowModel createFlow(FlowModel model) throws NullPointerException, ProxyException;
    FlowModel updateFlow(FlowModel FlowContent) throws NullPointerException, ProxyException;
    List<FlowModel> findAllFlows() throws ProxyException;
    FlowModel getFlow(Long id) throws ProxyException;

    // Flow Components
    FlowComponentModel createFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException;
    FlowComponentModel updateFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException;
    FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException;
    List<FlowComponentModel> findAllFlowComponents() throws ProxyException;
    FlowComponentModel getFlowComponent(Long id) throws ProxyException;

    // Flow Binders
    FlowBinderModel createFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException;
    FlowBinderModel updateFlowBinder(FlowBinderModel FlowBinderContent) throws NullPointerException, ProxyException;
    List<FlowBinderModel> findAllFlowBinders() throws ProxyException;
    FlowBinderModel getFlowBinder(long id) throws ProxyException;

    // Submitters
    SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;
    SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;
    void deleteSubmitter(long submitterId, long version) throws ProxyException;
    List<SubmitterModel> findAllSubmitters() throws ProxyException;
    SubmitterModel getSubmitter(Long id) throws ProxyException;

    // Sinks
    SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException;
    SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException;
    void deleteSink(long sinkId, long version) throws NullPointerException, ProxyException;

    List<SinkModel> findAllSinks() throws ProxyException;
    SinkModel getSink(Long id) throws ProxyException;

    // Other
    void close();

    class Factory {
        private static FlowStoreProxyAsync asyncInstance = null;
        public static FlowStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(FlowStoreProxy.class);
            }
            return asyncInstance;
        }
    }

}
