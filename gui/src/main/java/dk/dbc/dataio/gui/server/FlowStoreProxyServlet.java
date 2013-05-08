package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {
    private transient FlowStoreProxy flowStoreProxy;

    @Override
    public void init() throws ServletException {
        super.init();

        // ToDo: System.getProperty hack used for test purposes, plus figure out how to do context.xml like configurations with glassfish
        String flowStoreServiceEndpoint = System.getProperty("flowStoreURL");
	    if(flowStoreServiceEndpoint == null || flowStoreServiceEndpoint.isEmpty()) {
            ServletContext context = getServletContext();
            flowStoreServiceEndpoint = context.getInitParameter("flowStoreServiceEndpoint");
        }
        flowStoreProxy = new FlowStoreProxyImpl(flowStoreServiceEndpoint);
    }

    @Override
    public void createFlow(FlowData flowData) throws NullPointerException, IllegalStateException {
        flowStoreProxy.createFlow(flowData);
    }
}
