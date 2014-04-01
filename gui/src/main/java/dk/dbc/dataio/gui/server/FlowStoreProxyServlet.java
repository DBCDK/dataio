package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.*;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;

import javax.servlet.ServletException;
import java.util.List;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {
    private static final long serialVersionUID = 358109395377092219L;

    private transient FlowStoreProxy flowStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        flowStoreProxy = new FlowStoreProxyImpl();
    }

    @Override
    public void createFlow(FlowContent flowContent) throws NullPointerException, ProxyException {
        flowStoreProxy.createFlow(flowContent);
    }

    @Override
    public void createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException {
        flowStoreProxy.createFlowComponent(flowComponentContent);
    }

    @Override
    public void createSubmitter(SubmitterContent submitterContent) throws NullPointerException, ProxyException {
        flowStoreProxy.createSubmitter(submitterContent);
    }

    @Override
    public void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException {
        flowStoreProxy.createFlowBinder(flowBinderContent);
    }

    @Override
    public void createSink(SinkContent sinkContent) throws NullPointerException, ProxyException {
        flowStoreProxy.createSink(sinkContent);
    }

    @Override
    public void updateSink(Sink sink, Long id, Long version) throws NullPointerException, ProxyException {
        flowStoreProxy.updateSink(sink, id, version);
    }

    @Override
    public List<FlowComponent> findAllComponents() throws ProxyException {
        return flowStoreProxy.findAllComponents();
    }

    @Override
    public List<Submitter> findAllSubmitters() throws ProxyException {
        return flowStoreProxy.findAllSubmitters();
    }

    @Override
    public List<Flow> findAllFlows() throws ProxyException {
        return flowStoreProxy.findAllFlows();
    }

    @Override
    public List<Sink> findAllSinks() throws ProxyException {
        return flowStoreProxy.findAllSinks();
    }

    @Override
    public List<FlowBinder> findAllFlowBinders() throws ProxyException {
        return flowStoreProxy.findAllFlowBinders();
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
