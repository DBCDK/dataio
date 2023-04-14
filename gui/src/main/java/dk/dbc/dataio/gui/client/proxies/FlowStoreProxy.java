package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowBinderUsage;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.querylanguage.GwtQueryClause;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.DMatHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.InfomediaHarvesterConfig;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.PromatHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;

import java.util.List;

@RemoteServiceRelativePath("FlowStoreProxy")
public interface FlowStoreProxy extends RemoteService {
    // Flows
    FlowModel createFlow(FlowModel model) throws NullPointerException, ProxyException;

    FlowModel updateFlow(FlowModel FlowContent) throws NullPointerException, ProxyException;

    void deleteFlow(long flowId, long version) throws ProxyException;

    List<FlowModel> findAllFlows() throws ProxyException;

    FlowModel getFlow(Long id) throws ProxyException;

    // Flow Components
    FlowComponentModel createFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException;

    FlowComponentModel updateFlowComponent(FlowComponentModel model) throws NullPointerException, ProxyException;

    FlowModel refreshFlowComponents(Long id, Long version) throws NullPointerException, ProxyException;

    void deleteFlowComponent(long flowComponentId, long version) throws ProxyException;

    List<FlowComponentModel> findAllFlowComponents() throws ProxyException;

    FlowComponentModel getFlowComponent(Long id) throws ProxyException;

    // Flow Binders
    FlowBinderModel createFlowBinder(FlowBinderModel model) throws NullPointerException, ProxyException;

    FlowBinderModel updateFlowBinder(FlowBinderModel FlowBinderContent) throws NullPointerException, ProxyException;

    void deleteFlowBinder(long flowBinderId, long version) throws NullPointerException, ProxyException;

    List<FlowBinderModel> queryFlowBinders(List<GwtQueryClause> clauses) throws ProxyException;

    List<FlowBinderModel> findAllFlowBinders() throws ProxyException;

    List<FlowBinderUsage> getFlowBindersUsage() throws ProxyException;
    List<FlowBinderUsage> getFlowBindersUsageCached() throws ProxyException;

    FlowBinderModel getFlowBinder(long id) throws ProxyException;

    // Submitters
    SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;

    SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;

    void deleteSubmitter(long submitterId, long version) throws ProxyException;

    List<SubmitterModel> querySubmitters(List<GwtQueryClause> clauses) throws ProxyException;

    List<SubmitterModel> findAllSubmitters() throws ProxyException;

    SubmitterModel getSubmitter(Long id) throws ProxyException;

    List<FlowBinderIdent> getFlowBindersForSubmitter(long submitterId) throws ProxyException;

    // Sinks
    SinkModel createSink(SinkModel model) throws NullPointerException, ProxyException;

    SinkModel updateSink(SinkModel model) throws NullPointerException, ProxyException;

    void deleteSink(long sinkId, long version) throws NullPointerException, ProxyException;

    List<SinkModel> findAllSinks() throws ProxyException;

    SinkModel getSink(Long id) throws ProxyException;

    // Harvesters
    HarvesterConfig updateHarvesterConfig(HarvesterConfig config) throws ProxyException;

    void deleteHarvesterConfig(long id, long version) throws ProxyException;

    RRHarvesterConfig createRRHarvesterConfig(RRHarvesterConfig config) throws ProxyException;

    List<RRHarvesterConfig> findAllRRHarvesterConfigs() throws ProxyException;

    RRHarvesterConfig getRRHarvesterConfig(long id) throws ProxyException;

    TickleRepoHarvesterConfig createTickleRepoHarvesterConfig(TickleRepoHarvesterConfig config) throws ProxyException;

    List<TickleRepoHarvesterConfig> findAllTickleRepoHarvesterConfigs() throws ProxyException;

    TickleRepoHarvesterConfig getTickleRepoHarvesterConfig(long id) throws ProxyException;

    CoRepoHarvesterConfig createCoRepoHarvesterConfig(CoRepoHarvesterConfig config) throws ProxyException;

    List<CoRepoHarvesterConfig> findAllCoRepoHarvesterConfigs() throws ProxyException;

    CoRepoHarvesterConfig getCoRepoHarvesterConfig(long id) throws ProxyException;

    InfomediaHarvesterConfig createInfomediaHarvesterConfig(InfomediaHarvesterConfig config) throws ProxyException;

    List<InfomediaHarvesterConfig> findAllInfomediaHarvesterConfigs() throws ProxyException;

    InfomediaHarvesterConfig getInfomediaHarvesterConfig(long id) throws ProxyException;

    PeriodicJobsHarvesterConfig createPeriodicJobsHarvesterConfig(PeriodicJobsHarvesterConfig config) throws ProxyException;

    List<PeriodicJobsHarvesterConfig> findAllPeriodicJobsHarvesterConfigs() throws ProxyException;

    PeriodicJobsHarvesterConfig getPeriodicJobsHarvesterConfig(long id) throws ProxyException;

    List<PromatHarvesterConfig> findAllPromatHarvesterConfigs() throws ProxyException;

    PromatHarvesterConfig getPromatHarvesterConfig(long id) throws ProxyException;

    List<DMatHarvesterConfig> findAllDMatHarvesterConfigs() throws ProxyException;

    DMatHarvesterConfig getDMatHarvesterConfig(long id) throws ProxyException;

    // Gatekeeper destinations
    GatekeeperDestination createGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws NullPointerException, ProxyException;

    List<GatekeeperDestination> findAllGatekeeperDestinations() throws ProxyException;

    void deleteGatekeeperDestination(long id) throws ProxyException;

    GatekeeperDestination updateGatekeeperDestination(GatekeeperDestination gatekeeperDestination) throws ProxyException;

    // Other
    void close();

    class Factory {
        private static FlowStoreProxyAsync asyncInstance = null;

        public static FlowStoreProxyAsync getAsyncInstance() {
            if (asyncInstance == null) {
                asyncInstance = GWT.create(FlowStoreProxy.class);
            }
            return asyncInstance;
        }
    }

}
