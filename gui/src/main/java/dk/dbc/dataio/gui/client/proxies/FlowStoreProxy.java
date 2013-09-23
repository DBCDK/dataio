package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;

import java.util.List;

@RemoteServiceRelativePath("FlowStoreProxy")
public interface FlowStoreProxy extends RemoteService {

    void createFlow(FlowContent flowContent) throws NullPointerException, IllegalStateException;

    void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, IllegalStateException;

    void createSubmitter(SubmitterContent submitterContent) throws NullPointerException, IllegalStateException, FlowStoreProxyException;

    void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, IllegalStateException, FlowStoreProxyException;

    List<FlowComponent> findAllComponents();

    List<Submitter> findAllSubmitters();

    List<Flow> findAllFlows() throws Exception;
    
    public static class Factory {

        private static FlowStoreProxyAsync asyncInstance = null;

        public static FlowStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(FlowStoreProxy.class);
            }
            return asyncInstance;
        }
    }
}
