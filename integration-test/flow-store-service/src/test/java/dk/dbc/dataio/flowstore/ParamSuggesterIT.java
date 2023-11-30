package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ParameterSuggestion;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.PathBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ParamSuggesterIT extends AbstractFlowStoreServiceContainerTest {
    @BeforeClass
    public static void loadInitialState() {
        initializeDB();
    }

    @Test
    public void parameterSuggestions() throws JSONBException {
        assertThat("packaging", getSuggestion("PACKAGING"),
                is(new ParameterSuggestion()
                        .withName("packaging").withValues(List.of("iso", "json", "XML"))));

        assertThat("destination", getSuggestion("DESTINATION"),
                is(new ParameterSuggestion()
                        .withName("destination")
                        .withValues(List.of("basis", "broend3", "broend3-exttest", "broend3-loadtest",
                                "destination-1", "destination-2", "destination-3", "E4X", "XMLDOM"))));

        assertThat("charset", getSuggestion("CHARSET"),
                is(new ParameterSuggestion()
                        .withName("charset")
                        .withValues(List.of("utf-128", "utf-16", "utf-8"))));

        assertThat("format", getSuggestion("FORMAT"),
                is(new ParameterSuggestion()
                        .withName("format")
                        .withValues(List.of("basis", "format-1", "format-2", "format-3", "katalog"))));
    }

    private ParameterSuggestion getSuggestion(String parmName) throws JSONBException {
        HttpClient httpClient = HttpClient.create(HttpClient.newClient());
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(flowStoreServiceConnector.getBaseUrl())
                .withPathElements(
                        new PathBuilder(FlowStoreServiceConstants.PARAMETERS)
                                .bind(FlowStoreServiceConstants.PARM_VARIABLE, parmName)
                                .build());
        try (Response response = httpGet.execute()) {
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            return jsonbContext.unmarshall(response.readEntity(String.class), ParameterSuggestion.class);

        }
    }
}
