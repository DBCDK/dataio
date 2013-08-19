package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.engine.Flow;
import dk.dbc.dataio.engine.FlowComponent;
import dk.dbc.dataio.engine.FlowContent;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletException;
import java.util.List;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {

    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyServlet.class);
    private static final long serialVersionUID = 358109395377092219L;

    private transient FlowStoreProxy flowStoreProxy;

    @Override
    public void init() throws ServletException {
        super.init();

        final String flowStoreServiceEndpoint = ServletUtil.getFlowStoreServiceEndpoint();
        flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceEndpoint);
    }

    @Override
    public void createFlow(FlowContent flowContent) throws NullPointerException, IllegalStateException {
        flowStoreProxy.createFlow(flowContent);
    }

    @Override
    public void addFlowComponentToFlow(Flow flow, FlowComponent flowComponent) {
        flowStoreProxy.addFlowComponentToFlow(flow, flowComponent);
    }

    @Override
    public List<FlowComponent> findAllComponents() {
        return flowStoreProxy.findAllComponents();
    }
}
