package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
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
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;

import javax.naming.NamingException;
import javax.servlet.ServletException;
import java.util.List;

public class FlowStoreProxyServlet extends RemoteServiceServlet implements FlowStoreProxy {
    private static final long serialVersionUID = 358109395377092219L;

    private transient FlowStoreProxy flowStoreProxy = null;

    @Override
    public void init() throws ServletException {
        super.init();
        try{
            flowStoreProxy = new FlowStoreProxyImpl();
        }catch (NamingException e){
            throw new ServletException(e);
        }
    }

    @Override
    public Flow createFlow(FlowContent flowContent) throws NullPointerException, ProxyException {
        return flowStoreProxy.createFlow(flowContent);
    }

    @Override
    public FlowComponent createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException {
        return flowStoreProxy.createFlowComponent(flowComponentContent);
    }

    @Override
    public Submitter createSubmitter(SubmitterContent submitterContent) throws NullPointerException, ProxyException {
        return flowStoreProxy.createSubmitter(submitterContent);
    }

    @Override
    public void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException {
        flowStoreProxy.createFlowBinder(flowBinderContent);
    }

    @Override
    public Sink createSink(SinkContent sinkContent) throws NullPointerException, ProxyException {
        return flowStoreProxy.createSink(sinkContent);
    }

    @Override
    public Sink updateSink(SinkContent sinkContent, Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateSink(sinkContent, id, version);
    }

    @Override
    public List<FlowComponent> findAllFlowComponents() throws ProxyException {
        return flowStoreProxy.findAllFlowComponents();
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
    public Sink getSink(Long id) throws ProxyException {
        return flowStoreProxy.getSink(id);
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
