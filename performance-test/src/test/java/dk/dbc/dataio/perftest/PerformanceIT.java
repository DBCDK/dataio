package dk.dbc.dataio.perftest;

import dk.dbc.dataio.commons.types.SubmitterContent;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class PerformanceIT {

    @Test
    public void performanceTest() {
        String baseUrl = String.format("http://localhost:%s/flow-store", System.getProperty("glassfish.port"));
        Client restClient = HttpClient.newClient();

        // insert submitter
        // insert flowcomponent
        // insert flow
        // insert sink
        // insert flowbinder

        SubmitterContent submitterContent = new SubmitterContent(424242L, "perftestbib", "Library for performancetest");
        final Response response = HttpClient.doPostWithJson(restClient, submitterContent, baseUrl, FlowStoreServiceConstants.SUBMITTERS);
        assertThat(response.getStatusInfo().getStatusCode(), is(Response.Status.CREATED.getStatusCode()));
    }
}
