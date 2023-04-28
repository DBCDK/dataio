package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowBinderUsage;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

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
    public void deleteFlow(long flowId, long version) throws ProxyException {
        flowStoreProxy.deleteFlow(flowId, version);
    }

    @Override
    public List<FlowModel> findAllFlows() throws ProxyException {
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
    public FlowComponentModel createFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.createFlowComponent(model);
    }

    @Override
    public FlowComponentModel updateFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateFlowComponent(model);
    }

    @Override
    public FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException {
        return flowStoreProxy.refreshFlowComponents(id, version);
    }

    @Override
    public void deleteFlowComponent(long flowComponentId, long version) throws ProxyException {
        flowStoreProxy.deleteFlowComponent(flowComponentId, version);
    }

    @Override
    public List<FlowComponentModel> findAllFlowComponents() throws ProxyException {
        return flowStoreProxy.findAllFlowComponents();
    }

    @Override
    public FlowComponentModel getFlowComponent(Long id) throws ProxyException {
        return flowStoreProxy.getFlowComponent(id);
    }

    /*
     * Flows Binders
     */

    @Override
    public FlowBinderModel createFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.createFlowBinder(model);
    }

    @Override
    public FlowBinderModel updateFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException {
        return flowStoreProxy.updateFlowBinder(model);
    }

    @Override
    public void deleteFlowBinder(long flowBinderId, long version) throws NullPointerException, ProxyException {
        flowStoreProxy.deleteFlowBinder(flowBinderId, version);
    }

    @Override
    public List<FlowBinderModel> queryFlowBinders(List<GwtQueryClause> clauses) throws ProxyException {
        return flowStoreProxy.queryFlowBinders(clauses);
    }

    @Override
    public List<FlowBinderModel> findAllFlowBinders() throws ProxyException {
        return flowStoreProxy.findAllFlowBinders();
    }

    @Override
    public List<FlowBinderUsage> getFlowBindersUsage() throws ProxyException {
        return flowStoreProxy.getFlowBindersUsage();
    }

    @Override
    public List<FlowBinderUsage> getFlowBindersUsageCached() throws ProxyException {
        return flowStoreProxy.getFlowBindersUsageCached();
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
    public void deleteSubmitter(long submitterId, long version) throws ProxyException {
        flowStoreProxy.deleteSubmitter(submitterId, version);
    }

    @Override
    public List<SubmitterModel> querySubmitters(List<GwtQueryClause> clauses) throws ProxyException {
        return flowStoreProxy.querySubmitters(clauses);
    }

    @Override
    public List<SubmitterModel> findAllSubmitters() throws ProxyException {
        return flowStoreProxy.findAllSubmitters();
    }

    @Override
    public SubmitterModel getSubmitter(Long id) throws ProxyException {
        return flowStoreProxy.getSubmitter(id);
    }

    @Override
    public List<FlowBinderIdent> getFlowBindersForSubmitter(long submitterId) throws ProxyException {
        return flowStoreProxy.getFlowBindersForSubmitter(submitterId);
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
    public void deleteSink(long sinkId, long version) throws NullPointerException, ProxyException {
        flowStoreProxy.deleteSink(sinkId, version);
    }

    @Override
    public List<SinkModel> findAllSinks() throws ProxyException {
        return flowStoreProxy.findAllSinks();
    }

    @Override
    public SinkModel getSink(Long id) throws ProxyException {
        return flowStoreProxy.getSink(id);
    }


    /*
     * Harvesters
     */

    @Override
    public HarvesterConfig updateHarvesterConfig(HarvesterConfig config) throws ProxyException {
        return flowStoreProxy.updateHarvesterConfig(config);
    }

    @Override
    public void deleteHarvesterConfig(long id, long version) throws ProxyException {
        flowStoreProxy.deleteHarvesterConfig(id, version);
    }

    @Override
    public RRHarvesterConfig createRRHarvesterConfig(RRHarvesterConfig config) throws ProxyException {
        return flowStoreProxy.createRRHarvesterConfig(config);
    }

    @Override
    public List<RRHarvesterConfig> findAllRRHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllRRHarvesterConfigs();
    }

    @Override
    public RRHarvesterConfig getRRHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getRRHarvesterConfig(id);
    }

    @Override
    public TickleRepoHarvesterConfig createTickleRepoHarvesterConfig(TickleRepoHarvesterConfig config) throws ProxyException {
        return flowStoreProxy.createTickleRepoHarvesterConfig(config);
    }

    @Override
    public List<TickleRepoHarvesterConfig> findAllTickleRepoHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllTickleRepoHarvesterConfigs();
    }

    @Override
    public TickleRepoHarvesterConfig getTickleRepoHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getTickleRepoHarvesterConfig(id);
    }

    @Override
    public CoRepoHarvesterConfig createCoRepoHarvesterConfig(CoRepoHarvesterConfig config) throws ProxyException {
        return flowStoreProxy.createCoRepoHarvesterConfig(config);
    }

    @Override
    public List<CoRepoHarvesterConfig> findAllCoRepoHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllCoRepoHarvesterConfigs();
    }

    @Override
    public CoRepoHarvesterConfig getCoRepoHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getCoRepoHarvesterConfig(id);
    }

    @Override
    public InfomediaHarvesterConfig createInfomediaHarvesterConfig(InfomediaHarvesterConfig config) throws ProxyException {
        return flowStoreProxy.createInfomediaHarvesterConfig(config);
    }

    @Override
    public List<InfomediaHarvesterConfig> findAllInfomediaHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllInfomediaHarvesterConfigs();
    }

    @Override
    public InfomediaHarvesterConfig getInfomediaHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getInfomediaHarvesterConfig(id);
    }

    @Override
    public PeriodicJobsHarvesterConfig createPeriodicJobsHarvesterConfig(PeriodicJobsHarvesterConfig config) throws ProxyException {
        return flowStoreProxy.createPeriodicJobsHarvesterConfig(config);
    }

    @Override
    public List<PeriodicJobsHarvesterConfig> findAllPeriodicJobsHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllPeriodicJobsHarvesterConfigs();
    }

    @Override
    public PeriodicJobsHarvesterConfig getPeriodicJobsHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getPeriodicJobsHarvesterConfig(id);
    }

    @Override
    public List<PromatHarvesterConfig> findAllPromatHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllPromatHarvesterConfigs();
    }

    @Override
    public PromatHarvesterConfig getPromatHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getPromatHarvesterConfig(id);
    }

    @Override
    public List<DMatHarvesterConfig> findAllDMatHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllDMatHarvesterConfigs();
    }

    @Override
    public DMatHarvesterConfig getDMatHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getDMatHarvesterConfig(id);
    }

    /*
     * Gatekeeper destinations
     */

    @Override
    public GatekeeperDestination createGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws NullPointerException, ProxyException {
        return flowStoreProxy.createGatekeeperDestination(gatekeeperDestination);
    }

    @Override
    public List<GatekeeperDestination> findAllGatekeeperDestinations() throws ProxyException {
        return flowStoreProxy.findAllGatekeeperDestinations();
    }

    @Override
    public void deleteGatekeeperDestination(long id) throws ProxyException {
        flowStoreProxy.deleteGatekeeperDestination(id);
    }

    @Override
    public GatekeeperDestination updateGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws ProxyException {
        return flowStoreProxy.updateGatekeeperDestination(gatekeeperDestination);
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
