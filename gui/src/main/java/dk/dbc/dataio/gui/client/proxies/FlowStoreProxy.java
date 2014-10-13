package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;

import java.util.List;

@RemoteServiceRelativePath("FlowStoreProxy")
public interface FlowStoreProxy extends RemoteService {
    // Flows
    FlowModel createFlow(FlowModel model) throws NullPointerException, ProxyException;
    FlowModel updateFlow(FlowModel FlowContent) throws NullPointerException, ProxyException;
    List<Flow> findAllFlows() throws ProxyException;
    FlowModel getFlow(Long id) throws ProxyException;

    // Flow Components
    FlowComponent createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException;
    FlowComponent updateFlowComponent(FlowComponentContent flowComponentContent, Long id, Long version) throws NullPointerException, ProxyException;
    Flow refreshFlowComponentsOld(Long id, Long version) throws NullPointerException, ProxyException;
    FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException;
    List<FlowComponent> findAllFlowComponentsOld() throws ProxyException;
    List<FlowComponentModel> findAllFlowComponents() throws ProxyException;
    FlowComponent getFlowComponent(Long id) throws ProxyException;

    // Flow Binders
    FlowBinder createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException;
    List<FlowBinder> findAllFlowBinders() throws ProxyException;

    // Submitters
    SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;
    SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;
    List<Submitter> findAllSubmitters() throws ProxyException;
    SubmitterModel getSubmitter(Long id) throws ProxyException;

    // Sinks
    SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException;
    SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException;
    List<Sink> findAllSinks() throws ProxyException;
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
