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
import dk.dbc.dataio.common.utils.flowstore.FlowStoreServiceConnectorException;
import dk.dbc.dataio.common.utils.flowstore.ejb.FlowStoreServiceConnectorBean;
import dk.dbc.dataio.commons.types.ServiceError;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.harvester.types.UshSolrHarvesterConfig;
import dk.dbc.dataio.harvester.ush.solr.HarvestOperation;
import dk.dbc.dataio.jobstore.test.types.JobInfoSnapshotBuilder;
import dk.dbc.dataio.jobstore.types.JobInfoSnapshot;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Optional;

import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvesterApiBeanTest {

    private final static int USH_SOLR_HARVESTER_CONFIG_ID = 42;
    private final static String EXCEPTION_MSG = "msg";
    private final static String LOCATION = "location";

    private final FlowStoreServiceConnector flowStoreServiceConnector = mock(FlowStoreServiceConnector.class);
    private final HarvestOperation harvestOperation = mock(HarvestOperation.class);
    private final static UriInfo mockedUriInfo = mock(UriInfo.class);
    private final static UriBuilder uriBuilder = mock(UriBuilder.class);

    private HarvesterApiBean harvesterApiBean;
    private final JSONBContext jsonbContext = new JSONBContext();

    @BeforeClass
    public static void setupUri() throws URISyntaxException {
        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI(LOCATION));
    }

    @Before
    public void setup() {
        harvesterApiBean = initializeHarvesterApiBean();
    }

    @Test
    public void runTestHarvest_executeTestJobIsCreated_returnsResponseCreatedWithLocationSet() throws HarvesterException {
        Optional<JobInfoSnapshot> jobInfoSnapshot = Optional.of(new JobInfoSnapshotBuilder().build());
        doReturn(jobInfoSnapshot).when(harvestOperation).executeTest();

        // Subject under test
        Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, USH_SOLR_HARVESTER_CONFIG_ID);

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat("Response.location", response.getLocation().toString(), is(LOCATION));
        assertThat("Response.hasEntity", response.hasEntity(), is(false));
    }

    @Test
    public void runTestHarvest_ExecuteTestJobNotCreated_returnsResponseNoContent() throws HarvesterException {
        Optional<JobInfoSnapshot> jobInfoSnapshot = Optional.empty();
        doReturn(jobInfoSnapshot).when(harvestOperation).executeTest();

        // Subject under test
        Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, USH_SOLR_HARVESTER_CONFIG_ID);

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
        assertThat("Response.location", response.getLocation(), is(nullValue()));
        assertThat("Response.hasEntity", response.hasEntity(), is(false));
    }

    @Test
    public void runTestHarvest_executeTestThrows_returnsResponseInternalServerErrorWithServiceErrorAsEntity() throws HarvesterException {
        doThrow(new HarvesterException(EXCEPTION_MSG)).when(harvestOperation).executeTest();

        // Subject under test
        Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, USH_SOLR_HARVESTER_CONFIG_ID);

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat("Response.location", response.getLocation(), is(nullValue()));
        assertThat("Response.hasEntity", response.hasEntity(), is(true));

        assertServiceError((String) response.getEntity());
    }

    @Test
    public void runTestHarvest_getHarvesterConfigThrows_returnsResponseInternalServerErrorWithServiceErrorAsEntity() throws FlowStoreServiceConnectorException, JSONBException {
        when(flowStoreServiceConnector.getHarvesterConfig(anyLong(), eq(UshSolrHarvesterConfig.class))).thenThrow(new FlowStoreServiceConnectorException(EXCEPTION_MSG));

        // Subject under test
        Response response = harvesterApiBean.runTestHarvest(mockedUriInfo, USH_SOLR_HARVESTER_CONFIG_ID);

        // Verification
        assertThat("Response.status", response.getStatus(), is(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode()));
        assertThat("Response.location", response.getLocation(), is(nullValue()));
        assertThat("Response.hasEntity", response.hasEntity(), is(true));

        assertServiceError((String) response.getEntity());
    }

    /*
     * Private methods
     */

    private HarvesterApiBean initializeHarvesterApiBean() {
        try {
            HarvesterApiBean harvesterApiBean = Mockito.spy(new HarvesterApiBean());
            harvesterApiBean.flowStoreServiceConnectorBean =  mock(FlowStoreServiceConnectorBean.class);

            when(harvesterApiBean.flowStoreServiceConnectorBean.getConnector()).thenReturn(flowStoreServiceConnector);
            doReturn(harvestOperation).when(harvesterApiBean).getHarvestOperation(any(UshSolrHarvesterConfig.class));
            return harvesterApiBean;
        } catch (HarvesterException e) {
            throw new IllegalStateException("Error occurred while setting up partial spy for harvestOperation");
        }
    }

    private void assertServiceError(String entityString) {
        final ServiceError serviceError;
        try {
            serviceError = jsonbContext.unmarshall(entityString, ServiceError.class);
            assertThat("ServiceError.msg", serviceError.getMessage(), is(notNullValue()));
            assertThat("ServiceError.details", serviceError.getDetails(), is(EXCEPTION_MSG));
            assertThat("ServiceError.stacktrace", serviceError.getStacktrace(), is(notNullValue()));
        } catch (JSONBException e) {
            throw new IllegalStateException("Error occurred unmarshalling ServiceError");
        }

    }
}
