package dk.dbc.dataio.sink.es;

import com.sun.jersey.core.util.MultivaluedMapImpl;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkResultBuilder;
import org.apache.commons.codec.binary.Base64;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class EsMessageProcessorBeanIT {
    private static final String QUEUE_RESOURCE_ENDPOINT = "queue";
    private static final String ES_RESOURCE_ENDPOINT = "es";
    private static final String ES_INFLIGHT_RESOURCE_ENDPOINT = "es-inflight";
    private static final String ADDI_OK = "1\na\n1\nb\n";
    private static final int WAIT_PERIOD_IN_MS = 1000;
    private static final int NUMBER_OF_WAIT_PERIODS = 10;

    private static String esDatabase;
    private static String sinkqBaseUrl;
    private static Client restClient;

    @BeforeClass
    public static void setUpClass() throws SQLException, ClassNotFoundException, IOException {
        esDatabase = System.getProperty("es.dbname");
        sinkqBaseUrl = String.format("http://localhost:%s/sinkq", System.getProperty("glassfish.port"));
        restClient = HttpClient.newClient();
    }

    @Before
    public void createEsDatabase() {
        final MultivaluedMap<String, String> formData = new MultivaluedMapImpl();
        formData.add("dbname", esDatabase);
        HttpClient.doPostWithFormData(restClient, formData, sinkqBaseUrl, ES_RESOURCE_ENDPOINT);
    }

    @After
    public void removeEsDatabase() {
        HttpClient.doDelete(restClient, sinkqBaseUrl, ES_RESOURCE_ENDPOINT, esDatabase);
    }

    @Test
    public void happyPath() throws InterruptedException, SQLException, ClassNotFoundException {
        HttpClient.doPostWithJson(restClient, generateChunkResult(ADDI_OK), sinkqBaseUrl, QUEUE_RESOURCE_ENDPOINT);
        int numberOfTries = 0;
        while (numberOfTries < NUMBER_OF_WAIT_PERIODS) {
            Thread.sleep(WAIT_PERIOD_IN_MS);
            numberOfTries++;
            if (getQueueSize() == 0) {
                break;
            }
        }
        assertThat(getQueueSize(), is(0));
        assertThat(getNumberOfEsTargetReferences(), is(1));
        assertThat(getNumberOfEsInFlight(), is(1));
    }

    private int getQueueSize() {
        return getNumberFromHttpResponse(
                HttpClient.doGet(restClient, sinkqBaseUrl, QUEUE_RESOURCE_ENDPOINT));
    }

    private int getNumberOfEsTargetReferences() {
        return getNumberFromHttpResponse(
                HttpClient.doGet(restClient, sinkqBaseUrl, ES_RESOURCE_ENDPOINT, esDatabase));
    }

    private int getNumberOfEsInFlight() {
        return getNumberFromHttpResponse(
                HttpClient.doGet(restClient, sinkqBaseUrl, ES_INFLIGHT_RESOURCE_ENDPOINT));
    }

    private int getNumberFromHttpResponse(Response response) {
        int number = -1;
        if (response.getStatus() == Response.Status.OK.getStatusCode()) {
            final String responseContent = response.readEntity(String.class);
            number = Integer.parseInt(responseContent);
        }
        return number;
    }

    private String generateChunkResult(String record) {
        try {
            return JsonUtil.toJson(new ChunkResultBuilder()
                    .setResults(Arrays.asList(encodeBase64(record)))
                    .build());
        } catch (JsonException e) {
            throw new IllegalStateException(e);
        }
    }

    private String encodeBase64(String dataToEncode) {
        return Base64.encodeBase64String(dataToEncode.getBytes(StandardCharsets.UTF_8));
    }
}
