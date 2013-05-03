package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.gui.client.model.FlowData;
import dk.dbc.dataio.gui.client.proxy.FlowStoreProxy;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {
    private transient FlowStoreProxy flowStoreProxy = new FlowStoreProxyImpl();

    @Override
    public void createFlow(FlowData flowData) throws NullPointerException, IllegalStateException {
        flowStoreProxy.createFlow(flowData);
    }
}
