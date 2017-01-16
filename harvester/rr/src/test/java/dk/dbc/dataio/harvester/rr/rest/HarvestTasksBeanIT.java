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

package dk.dbc.dataio.harvester.rr.rest;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.rr.IntegrationTest;
import dk.dbc.dataio.harvester.rr.RawRepoRecordHarvestTask;
import dk.dbc.dataio.harvester.rr.entity.HarvestTask;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.dataio.harvester.types.HarvestRequest;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import javax.persistence.Query;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class HarvestTasksBeanIT extends IntegrationTest {
    private final UriInfo uriInfo = mock(UriInfo.class);
    private final UriBuilder uriBuilder = mock(UriBuilder.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private final long harvestId = 42;

    @Before
    public void setupMockedUriInfo() throws URISyntaxException {
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);
        when(uriBuilder.path(anyString())).thenReturn(uriBuilder);
        when(uriBuilder.build()).thenReturn(new URI("location"));
    }

    @Test
    public void requestIsIllegalJson() {
        final HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        final Response response = harvestTasksBean.createHarvestTask(uriInfo, harvestId, "not JSON");
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void requestIsInvalidJson() {
        final HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        final Response response = harvestTasksBean.createHarvestTask(uriInfo, harvestId, "{\"key\": \"value\"}");
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void requestIsUnknown() throws JSONBException {
        final HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        final Response response = harvestTasksBean.createHarvestTask(
                uriInfo, harvestId, jsonbContext.marshall(new UnknownRequest()));
        assertThat(response.getStatus(), is(Response.Status.BAD_REQUEST.getStatusCode()));
    }

    @Test
    public void taskIsCreated() {
        final List<AddiMetaData> expectedRecords = new ArrayList<>();
        expectedRecords.add(new AddiMetaData()
                        .withBibliographicRecordId("id1")
                        .withSubmitterNumber(123456)
                        .withLibraryRules(new AddiMetaData.LibraryRules()));
        expectedRecords.add(new AddiMetaData()
                        .withBibliographicRecordId("id2")
                        .withSubmitterNumber(654321)
                        .withLibraryRules(new AddiMetaData.LibraryRules()));

        final HarvestRecordsRequest request = new HarvestRecordsRequest(expectedRecords);

        final HarvestTasksBean harvestTasksBean = createHarvestTasksBean();
        final Response response = persistenceContext.run(() ->
                harvestTasksBean.createHarvestTask(uriInfo, harvestId, jsonbContext.marshall(request)));

        assertThat("Response status", response.getStatus(), is(Response.Status.CREATED.getStatusCode()));

        final Query query = entityManager
                .createQuery("SELECT task FROM HarvestTask task WHERE task.configId = :configId")
                .setParameter("configId", harvestId);

        final List<HarvestTask> created = query.getResultList();
        assertThat("Number of tasks created", created.size(), is(1));

        final HarvestTask task = created.get(0);
        assertThat("Task status", task.getStatus(), is(HarvestTask.Status.READY));
        assertThat("Task records", task.getRecords(), is(expectedRecords));
        assertThat("Task number of records", task.getNumberOfRecords(), is(expectedRecords.size()));
    }

    private HarvestTasksBean createHarvestTasksBean() {
        final HarvestTasksBean harvestTasksBean = new HarvestTasksBean();
        harvestTasksBean.entityManager = entityManager;
        return harvestTasksBean;
    }

    private static class UnknownRequest extends HarvestRequest<UnknownRequest> {
        private static final long serialVersionUID = -3753043123849598257L;
    }
}
