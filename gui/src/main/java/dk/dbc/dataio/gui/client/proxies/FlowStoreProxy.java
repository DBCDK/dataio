/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.gui.client.proxies;

import com.google.gwt.core.client.GWT;
import com.google.gwt.user.client.rpc.RemoteService;
import com.google.gwt.user.client.rpc.RemoteServiceRelativePath;
import dk.dbc.dataio.commons.types.FlowBinderWithSubmitter;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.PhHoldingsItemsHarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;

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

    List<FlowBinderModel> findAllFlowBinders() throws ProxyException;
    FlowBinderModel getFlowBinder(long id) throws ProxyException;

    // Submitters
    SubmitterModel createSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;
    SubmitterModel updateSubmitter(SubmitterModel model) throws NullPointerException, ProxyException;
    void deleteSubmitter(long submitterId, long version) throws ProxyException;
    List<SubmitterModel> findAllSubmitters() throws ProxyException;
    SubmitterModel getSubmitter(Long id) throws ProxyException;
    List<FlowBinderWithSubmitter> getFlowBindersForSubmitter(long submitterId) throws ProxyException;

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
    List<UshSolrHarvesterConfig> findAllUshSolrHarvesterConfigs() throws ProxyException;
    UshSolrHarvesterConfig getUshSolrHarvesterConfig(long id) throws ProxyException;
    TickleRepoHarvesterConfig createTickleRepoHarvesterConfig(TickleRepoHarvesterConfig config) throws ProxyException;
    List<TickleRepoHarvesterConfig> findAllTickleRepoHarvesterConfigs() throws ProxyException;
    TickleRepoHarvesterConfig getTickleRepoHarvesterConfig(long id) throws ProxyException;
    CoRepoHarvesterConfig createCoRepoHarvesterConfig(CoRepoHarvesterConfig config) throws ProxyException;
    List<CoRepoHarvesterConfig> findAllCoRepoHarvesterConfigs() throws ProxyException;
    CoRepoHarvesterConfig getCoRepoHarvesterConfig(long id) throws ProxyException;
    PhHoldingsItemsHarvesterConfig createHoldingsItemHarvesterConfig(PhHoldingsItemsHarvesterConfig config) throws ProxyException;
    List<PhHoldingsItemsHarvesterConfig> findAllHoldingsItemHarvesterConfigs() throws ProxyException;
    PhHoldingsItemsHarvesterConfig getHoldingsItemHarvesterConfig(long id) throws ProxyException;

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
