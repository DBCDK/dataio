package dk.dbc.dataio.harvester.task.connector;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.commons.types.rest.HarvesterServiceConstants;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.types.HarvestRecordsRequest;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpPost;
import dk.dbc.httpclient.PathBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;


public class HarvesterTaskServiceConnectorTest {
    private static final String SERVICE_URL = "http://dataio/harvester/xyz";
    private final FailSafeHttpClient failSafeHttpClient = mock(FailSafeHttpClient.class);

    private final HarvesterTaskServiceConnector rrHarvesterServiceConnector =
            new HarvesterTaskServiceConnector(failSafeHttpClient, SERVICE_URL);

    @Test(expected = HarvesterTaskServiceConnectorUnexpectedStatusCodeException.class)
    public void createHarvestTask_responseWithNotFoundStatusCode_throws() throws HarvesterTaskServiceConnectorException {
        createHarvestTask_mockedHttpWithSpecifiedReturnStatusCode(Response.Status.NOT_FOUND.getStatusCode(), null);
    }

    @Test
    public void createHarvestTask_harvestTaskCreated_returnsUri() throws HarvesterTaskServiceConnectorException {
        String taskId = createHarvestTask_mockedHttpWithSpecifiedReturnStatusCode(Response.Status.CREATED.getStatusCode(), "123");
        assertThat(taskId, is("123"));
    }

    private String createHarvestTask_mockedHttpWithSpecifiedReturnStatusCode(int statusCode, Object returnValue) throws HarvesterTaskServiceConnectorException {
        AddiMetaData addiMetaData = new AddiMetaData().withOcn("ocn").withPid("pid");
        HarvestRecordsRequest harvestRecordsRequest = new HarvestRecordsRequest(Collections.singletonList(addiMetaData))
                .withBasedOnJob(1);
        PathBuilder path = new PathBuilder(HarvesterServiceConstants.HARVEST_TASKS)
                .bind(HarvesterServiceConstants.HARVEST_ID_VARIABLE, 12L);

        HttpPost httpPost = new HttpPost(failSafeHttpClient)
                .withBaseUrl(SERVICE_URL)
                .withPathElements(path.build())
                .withJsonData(harvestRecordsRequest);

        when(failSafeHttpClient.execute(httpPost))
                .thenReturn(new MockedResponse<>(statusCode, returnValue));

        return rrHarvesterServiceConnector.createHarvestTask(12L, harvestRecordsRequest);
    }
}
