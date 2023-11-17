package dk.dbc.dataio.flowstore;

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.HttpGet;
import dk.dbc.httpclient.PathBuilder;
import jakarta.ws.rs.core.Response;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Objects;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class ParamSuggesterIT extends AbstractFlowStoreServiceContainerTest {
    @BeforeClass
    public static void loadInitialState() {
        initializeDB();
    }
    private static class TestingSuggestion {
        String name;
        List<String> values;

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public List<String> getValues() {
            return values;
        }

        public TestingSuggestion withName(String name) {
            this.name = name;
            return this;
        }
        public TestingSuggestion withValues(List<String> values) {
            this.values = values;
            return this;
        }

        public void setValues(List<String> values) {
            this.values = values;
        }

        @Override
        public String toString() {
            return "TestingSuggestion{" +
                    "name='" + name + '\'' +
                    ", values=" + values +
                    '}';
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            TestingSuggestion that = (TestingSuggestion) o;
            return Objects.equals(name, that.name) && Objects.equals(values, that.values);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name, values);
        }
    }
    private static final JSONBContext jsonbContext = new JSONBContext();

    @Test
    public void parameterSuggestions() throws JSONBException {
        assertThat("packaging", getSuggestion("packaging"),
                is(new TestingSuggestion()
                        .withName("packaging").withValues(List.of("iso", "json", "XML"))));

        assertThat("destination", getSuggestion("destination"),
                is(new TestingSuggestion()
                        .withName("destination")
                        .withValues(List.of("basis", "broend3", "broend3-exttest", "broend3-loadtest",
                                "destination-1", "destination-2", "destination-3", "E4X", "XMLDOM"))));

        assertThat("charset", getSuggestion("charset"),
                is(new TestingSuggestion()
                        .withName("charset")
                        .withValues(List.of("utf-128", "utf-16", "utf-8"))));

        assertThat("format", getSuggestion("format"),
                is(new TestingSuggestion()
                        .withName("format")
                        .withValues(List.of("basis", "format-1", "format-2", "format-3", "katalog"))));
    }

    private TestingSuggestion getSuggestion(String parmName) throws JSONBException {
        HttpClient httpClient = HttpClient.create(HttpClient.newClient());
        HttpGet httpGet = new HttpGet(httpClient)
                .withBaseUrl(flowStoreServiceConnector.getBaseUrl())
                .withPathElements(
                        new PathBuilder(FlowStoreServiceConstants.PARAMETERS)
                                .bind(FlowStoreServiceConstants.PARM_VARIABLE, parmName)
                                .build());
        try (Response response = httpGet.execute()) {
            assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
            return jsonbContext.unmarshall(response.readEntity(String.class), TestingSuggestion.class);

        }
    }
}
