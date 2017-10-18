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

import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnector;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ServiceError;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.harvester.ush.solr.HarvestOperation;
import dk.dbc.dataio.harvester.ush.solr.HarvesterConfigurationBean;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.util.Collections;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterApiBeanTest {
    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final HarvestOperation harvestOperation = mock(HarvestOperation.class);
    private final UriInfo mockedUriInfo = mock(UriInfo.class);

    private final UshSolrHarvesterConfig config = new UshSolrHarvesterConfig(42, 1, new UshSolrHarvesterConfig.Content());
    private final HarvesterException harvesterException = new HarvesterException("message");
    private final JSONBContext jsonbContext = new JSONBContext();

    private HarvesterApiBean harvesterApiBean;

    @Before
    public void setup() {
        harvesterApiBean = initializeHarvesterApiBean();
    }

    @Test
    public void runTestHarvest_noConfigurationFoundForId_returnsResponseNonFound() {
        // Subject under test
        final Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, config.getId() + 1);

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void runTestHarvest_jobIsCreated_returnsResponseCreatedWithLocationSet() throws HarvesterException {
        final int jobId = 123;
        Optional<JobInfoSnapshot> jobInfoSnapshot = Optional.of(new JobInfoSnapshot().withJobId(jobId));
        doReturn(jobInfoSnapshot).when(harvestOperation).executeTest();

        // Subject under test
        final Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, config.getId());

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat("Response.location", response.getLocation().toString(), is(Long.valueOf(jobId).toString()));
        assertThat("Response.hasEntity", response.hasEntity(), is(false));
    }

    @Test
    public void runTestHarvest_noJobCreated_returnsResponseNoContent() throws HarvesterException {
        final Optional<JobInfoSnapshot> jobInfoSnapshot = Optional.empty();
        doReturn(jobInfoSnapshot).when(harvestOperation).executeTest();

        // Subject under test
        final Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, config.getId());

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        assertThat("Response.location", response.getLocation(), is(nullValue()));
        assertThat("Response.hasEntity", response.hasEntity(), is(false));
    }

    @Test
    public void runTestHarvest_harvestOperationThrows_returnsResponseInternalServerErrorWithServiceErrorAsEntity() throws HarvesterException {
        doThrow(harvesterException).when(harvestOperation).executeTest();

        // Subject under test
        final Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, config.getId());

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat("Response.location", response.getLocation(), is(nullValue()));
        assertThat("Response.hasEntity", response.hasEntity(), is(true));

        assertServiceError((String) response.getEntity());
    }

    private HarvesterApiBean initializeHarvesterApiBean() {
        try {
            final HarvesterApiBean harvesterApiBean = Mockito.spy(new HarvesterApiBean());
            harvesterApiBean.flowStoreServiceConnectorBean =  mock(FlowStoreServiceConnectorBean.class);
            harvesterApiBean.configs = mock(HarvesterConfigurationBean.class);

            when(harvesterApiBean.configs.get()).thenReturn(Collections.singletonList(config));
            when(harvesterApiBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
            doReturn(harvestOperation).when(harvesterApiBean).getHarvestOperation(any(UshSolrHarvesterConfig.class));
            return harvesterApiBean;
        } catch (HarvesterException e) {
            throw new IllegalStateException("Error occurred while setting up partial spy for harvestOperation");
        }
    }

    private void assertServiceError(String entityString) {
        try {
            final ServiceError serviceError = jsonbContext.unmarshall(entityString, ServiceError.class);
            assertThat("ServiceError.msg", serviceError.getMessage(), is(notNullValue()));
            assertThat("ServiceError.details", serviceError.getDetails(), is(harvesterException.getMessage()));
            assertThat("ServiceError.stacktrace", serviceError.getStacktrace(), is(notNullValue()));
        } catch (JSONBException e) {
            throw new IllegalStateException("Error occurred while unmarshalling ServiceError");
        }
    }
}
