package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
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
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import java.util.List;

@RemoteServiceRelativePath("FlowStoreProxy")
public interface FlowStoreProxy extends RemoteService {

    void createFlow(FlowContent flowContent) throws NullPointerException, ProxyException;
    void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException;
    void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException;
    void createSubmitter(SubmitterContent submitterContent) throws NullPointerException, ProxyException;
    Sink createSink(SinkContent sinkContent) throws NullPointerException, ProxyException;

    Sink updateSink(SinkContent sinkContent, Long id, Long version) throws NullPointerException, ProxyException;

    List<Flow> findAllFlows() throws ProxyException;
    List<FlowBinder> findAllFlowBinders() throws ProxyException;
    List<FlowComponent> findAllComponents() throws ProxyException;
    List<Submitter> findAllSubmitters() throws ProxyException;
    List<Sink> findAllSinks() throws ProxyException;

    Sink getSink(Long id) throws ProxyException;

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
