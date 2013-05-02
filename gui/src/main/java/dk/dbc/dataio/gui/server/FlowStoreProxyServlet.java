package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.model.Flow;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {
    private FlowStoreProxy flowStoreProxy = new FlowStoreProxyImpl();

    @Override
    public void createFlow(Flow flow) throws NullPointerException, IllegalStateException {
        flowStoreProxy.createFlow(flow);
    }
}
