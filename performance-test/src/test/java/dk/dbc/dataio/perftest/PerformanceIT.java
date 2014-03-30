package dk.dbc.dataio.perftest;

import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PerformanceIT {

    @Test
    public void performanceTest() throws JsonException {
        String baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        Client restClient = HttpClient.newClient();

        // insert submitter
        SubmitterContent submitterContent = new SubmitterContent(424242L, "perftestbib", "Library for performancetest");
        final String submitterJson = JsonUtil.toJson(submitterContent);
        final Response response = HttpClient.doPostWithJson(restClient, submitterJson, baseUrl, FlowStoreServiceConstants.SUBMITTERS);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));

        // insert flowcomponent with javascript with no functionality
        // insert flow
        // insert sink
        // insert flowbinder


        // Start timer
        // Create Job
        // Insert Job
        // Wait for Job-completion
        // End Timer
        // Somehow write result of timer in useful format
    }
}
