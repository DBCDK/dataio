package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.FlowBinder;
import dk.dbc.dataio.commons.types.FlowBinderContent;
import dk.dbc.dataio.commons.types.FlowComponent;
import dk.dbc.dataio.commons.types.FlowComponentContent;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.Submitter;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.pages.flow.modify.FlowModel;
import dk.dbc.dataio.gui.client.pages.flowbinder.modify.FlowBinderModel;
import dk.dbc.dataio.gui.client.pages.flowcomponent.modify.FlowComponentModel;
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
        try {
            flowStoreProxy = new FlowStoreProxyImpl();
        } catch (NamingException e) {
            throw new ServletException(e);
        }
    }


    /*
     * Flows
     */

    @Override
    public FlowModel createFlow(FlowModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.createFlow(model);
    }

    @Override
    public FlowModel updateFlow(FlowModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateFlow(model);
    }

    @Override
    public List<Flow> findAllFlows() throws ProxyException {
        return flowStoreProxy.findAllFlows();
    }

    @Override
    public FlowModel getFlow(Long id) throws ProxyException {
        return flowStoreProxy.getFlow(id);
    }


    /*
     * Flow Components
     */

    @Override
    public FlowComponent createFlowComponent(FlowComponentContent flowComponentContent) throws NullPointerException, ProxyException {
        return flowStoreProxy.createFlowComponent(flowComponentContent);
    }

    @Override
    public FlowComponent updateFlowComponent(FlowComponentContent flowComponentContent, Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateFlowComponent(flowComponentContent, id, version);
    }

    @Override
    public Flow refreshFlowComponentsOld(Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.refreshFlowComponentsOld(id, version);
    }

    @Override
    public FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.refreshFlowComponents(id, version);
    }

    @Override
    public List<FlowComponent> findAllFlowComponentsOld() throws ProxyException {
        return flowStoreProxy.findAllFlowComponentsOld();
    }

    @Override
    public List<FlowComponentModel> findAllFlowComponents() throws ProxyException {
        return flowStoreProxy.findAllFlowComponents();
    }

    @Override
    public FlowComponent getFlowComponent(Long id) throws ProxyException {
        return flowStoreProxy.getFlowComponent(id);
    }


    /*
     * Flows Binders
     */

    @Override
    public FlowBinder createFlowBinder(FlowBinderContent flowBinderContent) throws NullPointerException, ProxyException {
        return flowStoreProxy.createFlowBinder(flowBinderContent);
    }

    @Override
    public List<FlowBinder> findAllFlowBinders() throws ProxyException {
        return flowStoreProxy.findAllFlowBinders();
    }

    @Override
    public FlowBinderModel getFlowBinder(long id) throws ProxyException {
        return flowStoreProxy.getFlowBinder(id);
    }


    /*
     * Submitters
     */

    @Override
    public SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.createSubmitter(model);
    }

    @Override
    public SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateSubmitter(model);
    }

    @Override
    public List<Submitter> findAllSubmitters() throws ProxyException {
        return flowStoreProxy.findAllSubmitters();
    }

    @Override
    public SubmitterModel getSubmitter(Long id) throws ProxyException {
        return flowStoreProxy.getSubmitter(id);
    }


    /*
     * Sinks
     */

    @Override
    public SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.createSink(model);
    }

    @Override
    public SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateSink(model);
    }

    @Override
    public List<Sink> findAllSinks() throws ProxyException {
        return flowStoreProxy.findAllSinks();
    }

    @Override
    public SinkModel getSink(Long id) throws ProxyException {
        return flowStoreProxy.getSink(id);
    }


    /*
     * Other
     */

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
