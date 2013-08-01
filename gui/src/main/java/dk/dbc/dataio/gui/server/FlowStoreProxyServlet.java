package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.servlet.ServletException;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {
    private static final Logger log = LoggerFactory.getLogger(FlowStoreProxyServlet.class);
    private static final long serialVersionUID = 358109395377092219L;
    private static final String FLOW_STORE_SERVICE_ENDPOINT_RESOURCE = "dataioGuiFlowStoreServiceEndpoint";

    private transient FlowStoreProxy flowStoreProxy;

    @Override
    public void init() throws ServletException {
        super.init();

        // ToDo: System.getProperty hack used for test purposes
        String flowStoreServiceEndpoint = System.getProperty("flowStoreURL");
        if (flowStoreServiceEndpoint == null || flowStoreServiceEndpoint.isEmpty()) {
            InitialContext initialContext = null;
            try {
                initialContext = new InitialContext();
                flowStoreServiceEndpoint = (String) initialContext.lookup(FLOW_STORE_SERVICE_ENDPOINT_RESOURCE);
            } catch (NamingException e) {
                throw new ServletException(e);
            } finally {
                closeInitialContext(initialContext);
            }
        }
        flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceEndpoint);
    }

    @Override
    public void createFlow(FlowData flowData) throws NullPointerException, IllegalStateException {
        flowStoreProxy.createFlow(flowData);
    }

    private void closeInitialContext(InitialContext initialContext) {
        if (initialContext != null) {
            try {
                initialContext.close();
            } catch (NamingException e) {
                log.warn("Unable to close initial context", e);
            }
        }
    }
}
