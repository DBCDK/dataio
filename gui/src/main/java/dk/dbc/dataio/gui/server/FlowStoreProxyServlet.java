package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.FlowContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.sink.modify.SinkModel;
import dk.dbc.dataio.gui.client.pages.submitter.modify.SubmitterModel;
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
    public SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.createSubmitter(model);
    }

    @Override
    public void createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException {
        flowStoreProxy.createFlowBinder(flowBinderContent);
    }

    @Override
    public SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.createSink(model);
    }

    @Override
    public SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateSink(model);
    }

    @Override
    public SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateSubmitter(model);
    }

    @Override
    public FlowComponent updateFlowComponent(FlowComponentContent flowComponentContent, Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateFlowComponent(flowComponentContent, id, version);
    }

    @Override
    public FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.refreshFlowComponents(id, version);
    }

    @Override
    public Flow refreshFlowComponentsOld(Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.refreshFlowComponentsOld(id, version);
    }

    @Override
    public FlowModel updateFlow(FlowModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateFlow(model);
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
    public SinkModel getSink(Long id) throws ProxyException {
        return flowStoreProxy.getSink(id);
    }

    @Override
    public SubmitterModel getSubmitter(Long id) throws ProxyException {
        return flowStoreProxy.getSubmitter(id);
    }

    @Override
    public FlowComponent getFlowComponent(Long id) throws ProxyException {
        return flowStoreProxy.getFlowComponent(id);
    }

    @Override
    public FlowModel getFlow(Long id) throws ProxyException {
        return flowStoreProxy.getFlow(id);
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
