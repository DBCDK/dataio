package dk.dbc.dataio.commons.creatordetector.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.httpclient.FailSafeHttpClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class CreatorDetectorConnectorTest {

    private static WireMockServer wireMockServer;
    private static CreatorDetectorConnector connector;

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        connector = CreatorDetectorConnectorFactory.create("http://localhost:" + wireMockServer.port());
    }

    @AfterAll
    static void tearDown() {
        connector.close();
        wireMockServer.stop();
    }

    private final DetectCreatorNamesRequest detectCreatorNamesRequest =
            new DetectCreatorNamesRequest("Kim Skotte, journalist", "123456789");

    @Test
    void constructor_failSafeHttpClientArgIsNull_throws() {
        assertThrows(NullPointerException.class, () ->
                new CreatorDetectorConnector((FailSafeHttpClient) null, "http://localhost"));
    }

    @Test
    void constructor_baseUrlArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> {
            try (Client client = ClientBuilder.newClient()) {
                new CreatorDetectorConnector(client, null);
            }
        });
    }

    @Test
    void constructor_baseUrlArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (Client client = ClientBuilder.newClient()) {
                new CreatorDetectorConnector(client, " ");
            }
        });
    }

    @Test
    void detectCreatorNames_serviceReturnsOk_returnsCreatorNameSuggestions() throws CreatorDetectorConnectorException {
        String responseBody = """
            {
              "results": [
                {
                  "authority_id": "870979:68943574",
                  "detected_ner_name": "kim skotte",
                  "authority_name_normalized": "kim skotte",
                  "match_score": 0.873639702796936,
                  "rerank_score": 7.805474625270857
                }
              ]
            }
            """;

        wireMockServer.stubFor(post(urlEqualTo("/detect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        CreatorNameSuggestions result = connector.detectCreatorNames(detectCreatorNamesRequest);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/detect"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {
                          "query": "Kim Skotte, journalist",
                          "infomedia_id": "123456789"
                        }
                        """)));

        final CreatorNameSuggestions expectedCreatorNameSuggestions = new CreatorNameSuggestions();
        final CreatorNameSuggestion expectedCreatorNameSuggestion = new CreatorNameSuggestion(
                "kim skotte", "870979:68943574", "kim skotte", 0.873639702796936, 7.805474625270857);
        expectedCreatorNameSuggestions.setResults(List.of(expectedCreatorNameSuggestion));

        assertThat(result, is(expectedCreatorNameSuggestions));
    }

    @Test
    void detectCreatorNames_serviceReturnsEmptyResults_returnsEmptyCreatorNameSuggestions() throws CreatorDetectorConnectorException {
        String responseBody = """
                {
                    "results": []
                }
                """;

        wireMockServer.stubFor(post(urlEqualTo("/detect"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        CreatorNameSuggestions result = connector.detectCreatorNames(detectCreatorNamesRequest);

        assertNotNull(result);
        assertNotNull(result.getResults());
        assertEquals(0, result.getResults().size());
    }

    @Test
    void detectCreatorNames_serviceReturnsBadRequest_throws() {
        wireMockServer.stubFor(post(urlEqualTo("/detect"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")));

        CreatorDetectorConnectorUnexpectedStatusCodeException exception = assertThrows(
                CreatorDetectorConnectorUnexpectedStatusCodeException.class,
                () -> connector.detectCreatorNames(detectCreatorNamesRequest));

        assertEquals(400, exception.getStatusCode());
    }
}
