package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.gui.client.exceptions.FlowStoreProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.List;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {

    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyServlet.class);
    private static final long serialVersionUID = 358109395377092219L;

    private transient FlowStoreProxy flowStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        flowStoreProxy = new FlowStoreProxyImpl();
    }

    @Override
    public void createFlow(FlowContent flowContent) throws NullPointerException, FlowStoreProxyException {
        flowStoreProxy.createFlow(flowContent);
    }

    @Override
    public void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, FlowStoreProxyException {
        flowStoreProxy.createFlowComponent(flowComponentContent);
    }

    @Override
    public void createSubmitter(SubmitterContent submitterContent) throws NullPointerException, FlowStoreProxyException {
        flowStoreProxy.createSubmitter(submitterContent);
    }

    @Override
    public void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, FlowStoreProxyException {
        flowStoreProxy.createFlowBinder(flowBinderContent);
    }

    @Override
    public void createSink(SinkContent sinkContent) throws NullPointerException, FlowStoreProxyException {
        flowStoreProxy.createSink(sinkContent);
    }

    @Override
    public List<FlowComponent> findAllComponents() throws FlowStoreProxyException {
        return flowStoreProxy.findAllComponents();
    }

    @Override
    public List<Submitter> findAllSubmitters() throws FlowStoreProxyException {
        return flowStoreProxy.findAllSubmitters();
    }

    @Override
    public List<Flow> findAllFlows() throws FlowStoreProxyException {
        return flowStoreProxy.findAllFlows();
    }

    @Override
    public void close() {
        if (flowStoreProxy != null) {
            flowStoreProxy.close();
            flowStoreProxy = null;
        }
    }

    @Override
    public void destroy() {
        super.destroy();
        close();
    }

}
