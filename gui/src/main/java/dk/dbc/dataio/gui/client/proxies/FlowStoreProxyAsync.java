package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.user.client.rpc.AsyncCallback;
import dk.dbc.dataio.commons.types.FlowBinderIdent;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
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

public interface FlowStoreProxyAsync {
    // Flows
    void createFlow(FlowModel model, AsyncCallback<FlowModel> async);

    void updateFlow(FlowModel model, AsyncCallback<FlowModel> async);

    void deleteFlow(long flowId, long version, AsyncCallback<Void> async);

    void findAllFlows(AsyncCallback<List<FlowModel>> async);

    void getFlow(Long id, AsyncCallback<FlowModel> async);

    // Flow Components
    void createFlowComponent(FlowComponentModel model, AsyncCallback<FlowComponentModel> async);

    void updateFlowComponent(FlowComponentModel model, AsyncCallback<FlowComponentModel> async);

    void refreshFlowComponents(Long id, Long version, AsyncCallback<FlowModel> async);

    void deleteFlowComponent(long flowComponentId, long version, AsyncCallback<Void> async);

    void findAllFlowComponents(AsyncCallback<List<FlowComponentModel>> async);

    void getFlowComponent(Long id, AsyncCallback<FlowComponentModel> async);

    // Flow Binders
    void createFlowBinder(FlowBinderModel model, AsyncCallback<FlowBinderModel> async);

    void updateFlowBinder(FlowBinderModel model, AsyncCallback<FlowBinderModel> async);

    void deleteFlowBinder(long flowBinderId, long version, AsyncCallback<Void> async);

    void queryFlowBinders(List<GwtQueryClause> clauses, AsyncCallback<List<FlowBinderModel>> async);

    void findAllFlowBinders(AsyncCallback<List<FlowBinderModel>> async);
    void getFlowBindersUsage(AsyncCallback<List<FlowBinderUsage>> arg1);
    void getFlowBindersUsageCached(AsyncCallback<List<FlowBinderUsage>> arg1);
    void getFlowBinder(long id, AsyncCallback<FlowBinderModel> async);

    // Submitters
    void createSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);

    void updateSubmitter(SubmitterModel model, AsyncCallback<SubmitterModel> async);

    void deleteSubmitter(long submitterId, long version, AsyncCallback<Void> async);

    void querySubmitters(List<GwtQueryClause> clauses, AsyncCallback<List<SubmitterModel>> async);

    void findAllSubmitters(AsyncCallback<List<SubmitterModel>> async);

    void getSubmitter(Long id, AsyncCallback<SubmitterModel> async);

    void getFlowBindersForSubmitter(long submitterId, AsyncCallback<List<FlowBinderIdent>> async);

    // Sinks
    void createSink(SinkModel model, AsyncCallback<SinkModel> async);

    void updateSink(SinkModel model, AsyncCallback<SinkModel> async);

    void deleteSink(long sinkId, long version, AsyncCallback<Void> async);

    void findAllSinks(AsyncCallback<List<SinkModel>> async);

    void getSink(Long id, AsyncCallback<SinkModel> async);

    // Harvesters
    void updateHarvesterConfig(HarvesterConfig config, AsyncCallback<HarvesterConfig> async);

    void deleteHarvesterConfig(long id, long version, AsyncCallback<Void> async);

    void createRRHarvesterConfig(RRHarvesterConfig config, AsyncCallback<RRHarvesterConfig> async);

    void findAllRRHarvesterConfigs(AsyncCallback<List<RRHarvesterConfig>> async);

    void getRRHarvesterConfig(long id, AsyncCallback<RRHarvesterConfig> async);

    void createTickleRepoHarvesterConfig(TickleRepoHarvesterConfig config, AsyncCallback<TickleRepoHarvesterConfig> async);

    void findAllTickleRepoHarvesterConfigs(AsyncCallback<List<TickleRepoHarvesterConfig>> async);

    void getTickleRepoHarvesterConfig(long id, AsyncCallback<TickleRepoHarvesterConfig> async);

    void createCoRepoHarvesterConfig(CoRepoHarvesterConfig config, AsyncCallback<CoRepoHarvesterConfig> async);

    void findAllCoRepoHarvesterConfigs(AsyncCallback<List<CoRepoHarvesterConfig>> async);

    void getCoRepoHarvesterConfig(long id, AsyncCallback<CoRepoHarvesterConfig> async);

    void createInfomediaHarvesterConfig(InfomediaHarvesterConfig config, AsyncCallback<InfomediaHarvesterConfig> async);

    void findAllInfomediaHarvesterConfigs(AsyncCallback<List<InfomediaHarvesterConfig>> async);

    void getInfomediaHarvesterConfig(long id, AsyncCallback<InfomediaHarvesterConfig> async);

    void createPeriodicJobsHarvesterConfig(PeriodicJobsHarvesterConfig config, AsyncCallback<PeriodicJobsHarvesterConfig> async);

    void findAllPeriodicJobsHarvesterConfigs(AsyncCallback<List<PeriodicJobsHarvesterConfig>> async);

    void getPeriodicJobsHarvesterConfig(long id, AsyncCallback<PeriodicJobsHarvesterConfig> async);

    void findAllPromatHarvesterConfigs(AsyncCallback<List<PromatHarvesterConfig>> async);

    void getPromatHarvesterConfig(long id, AsyncCallback<PromatHarvesterConfig> async);

    void findAllDMatHarvesterConfigs(AsyncCallback<List<DMatHarvesterConfig>> async);

    void getDMatHarvesterConfig(long id, AsyncCallback<DMatHarvesterConfig> async);

    // Gatekeeper destinations
    void createGatekeeperDestination(GatekeeperDestination gatekeeperDestination, AsyncCallback<GatekeeperDestination> async);

    void findAllGatekeeperDestinations(AsyncCallback<List<GatekeeperDestination>> async);

    void deleteGatekeeperDestination(long id, AsyncCallback<Void> async);

    void updateGatekeeperDestination(GatekeeperDestination gatekeeperDestination, AsyncCallback<GatekeeperDestination> async);

    // Other
    void close(AsyncCallback<Void> async);

    // Harvesters

}
