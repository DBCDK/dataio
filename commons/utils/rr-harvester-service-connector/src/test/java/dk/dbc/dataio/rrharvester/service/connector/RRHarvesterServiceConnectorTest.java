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

package dk.dbc.dataio.rrharvester.service.connector;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.rest.RRHarvesterServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.FailSafeHttpClient;
import dk.dbc.dataio.commons.utils.httpclient.HttpPost;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import org.junit.Test;

import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class RRHarvesterServiceConnectorTest {
    private static final String SERVICE_URL = "http://dataio/harvester/rr";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final RRHarvesterServiceConnector rrHarvesterServiceConnector =
            new RRHarvesterServiceConnector(failSafeHttpClient, SERVICE_URL);

    @Test(expected = RRHarvesterServiceConnectorUnexpectedStatusCodeException.class)
    public void createHarvestTask_responseWithNotFoundStatusCode_throws() throws RRHarvesterServiceConnectorException {
        createHarvestTask_mockedHttpWithSpecifiedReturnStatusCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    @Test
    public void createHarvestTask_harvestTaskCreated_returnsUri() throws RRHarvesterServiceConnectorException {
        final String taskId = createHarvestTask_mockedHttpWithSpecifiedReturnStatusCode(Response.Status.CREATED.getStatusCode(), "123");
        assertThat(taskId, is("123"));
    }

    private String createHarvestTask_mockedHttpWithSpecifiedReturnStatusCode(int statusCode, Object returnValue) throws RRHarvesterServiceConnectorException {
        final AddiMetaData addiMetaData = new AddiMetaData().withOcn("ocn").withPid("pid");
        final HarvestRecordsRequest harvestRecordsRequest = new HarvestRecordsRequest(Collections.singletonList(addiMetaData))
                .withBasedOnJob(1);
        final PathBuilder path = new PathBuilder(RRHarvesterServiceConstants.HARVEST_TASKS)
                .bind(RRHarvesterServiceConstants.HARVEST_ID_VARIABLE, 12L);

        final HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(SERVICE_URL)
                .withPathElements(path.build())
                .withJsonData(harvestRecordsRequest);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return rrHarvesterServiceConnector.createHarvestTask(12L, harvestRecordsRequest);
    }
}
