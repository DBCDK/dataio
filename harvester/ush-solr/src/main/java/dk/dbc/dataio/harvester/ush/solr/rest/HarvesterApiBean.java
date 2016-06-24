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

package dk.dbc.dataio.harvester.ush.solr.rest;

import dk.dbc.dataio.bfs.ejb.BinaryFileStoreBean;
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.rest.UshServiceConstants;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.filestore.service.connector.ejb.FileStoreServiceConnectorBean;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.harvester.ush.solr.HarvestOperation;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.util.Optional;

@Stateless
@Path("/")
public class HarvesterApiBean {

    private static final Logger LOGGER = LoggerFactory.getLogger(HarvesterApiBean.class);

    @EJB
    public BinaryFileStoreBean binaryFileStoreBean;

    @EJB
    public FileStoreServiceConnectorBean fileStoreServiceConnectorBean;

    @EJB
    public JobStoreServiceConnectorBean jobStoreServiceConnectorBean;

    @EJB 
    public FlowStoreServiceConnectorBean flowStoreServiceConnectorBean;


    @POST
    @Path(UshServiceConstants.HARVESTERS_USH_SOLR_TEST)
    @Produces({MediaType.APPLICATION_JSON})
    public Response runTestHarvest(@Context UriInfo uriInfo, @PathParam(UshServiceConstants.ID_VARIABLE) int id) {
        LOGGER.trace("Called with ushSolrHarvesterConfig.id: '{}'", id);
        try {
            UshSolrHarvesterConfig ushSolrHarvesterConfig = flowStoreServiceConnectorBean.getConnector().getHarvesterConfig(id, UshSolrHarvesterConfig.class);
            HarvestOperation harvestOperation = getHarvestOperation(ushSolrHarvesterConfig);
            Optional<JobInfoSnapshot> jobInfoSnapshot = harvestOperation.executeTest();

            if (jobInfoSnapshot.isPresent()) {
                return Response.created(getUri(uriInfo, Integer.toString(jobInfoSnapshot.get().getJobId())))
                        .build();
            } else {
                return Response.noContent().build();
            }
        } catch (FlowStoreServiceConnectorException | HarvesterException e) {
            return Response.status(Response.Status.INTERNAL_SERVER_ERROR).entity(ServiceUtil.asJsonError(e, "Error occurred while executing test harvest")).build();
        }
    }

    private URI getUri(UriInfo uriInfo, String jobId) {
        final UriBuilder absolutePathBuilder = uriInfo.getAbsolutePathBuilder();
        return absolutePathBuilder.path(jobId).build();
    }

    public HarvestOperation getHarvestOperation(UshSolrHarvesterConfig config) throws HarvesterException {
        return new HarvestOperation(config, flowStoreServiceConnectorBean.getConnector(),
                binaryFileStoreBean, fileStoreServiceConnectorBean.getConnector(), jobStoreServiceConnectorBean.getConnector());
    }
}
