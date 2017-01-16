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

package dk.dbc.dataio.gui.server;

import com.google.gwt.user.server.rpc.RemoteServiceServlet;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.gui.client.model.FlowBinderModel;
import dk.dbc.dataio.gui.client.model.FlowComponentModel;
import dk.dbc.dataio.gui.client.model.FlowModel;
import dk.dbc.dataio.gui.client.model.SinkModel;
import dk.dbc.dataio.gui.client.model.SubmitterModel;
import dk.dbc.dataio.gui.client.proxies.FlowStoreProxy;
import dk.dbc.dataio.harvester.types.CoRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.harvester.types.TickleRepoHarvesterConfig;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;

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
    public List<FlowBinderModel> findAllFlowBinders() throws ProxyException {
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
    public void deleteSubmitter(long submitterId, long version) throws ProxyException {
        flowStoreProxy.deleteSubmitter(submitterId, version);
    }

    @Override
    public List<SubmitterModel> findAllSubmitters() throws ProxyException {
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
    public List<UshSolrHarvesterConfig> findAllUshSolrHarvesterConfigs() throws ProxyException {
        return flowStoreProxy.findAllUshSolrHarvesterConfigs();
    }

    @Override
    public UshSolrHarvesterConfig getUshSolrHarvesterConfig(long id) throws ProxyException {
        return flowStoreProxy.getUshSolrHarvesterConfig(id);
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
