package dk.dbc.dataio.commons.retriever.connector;

import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.dataio.commons.retriever.connector.model.ArticlesRequest;
import dk.dbc.dataio.commons.retriever.connector.model.ArticlesResponse;
import dk.dbc.httpclient.FailSafeHttpClient;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.client.ClientBuilder;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
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
import static org.junit.jupiter.api.Assertions.assertThrows;

class RetrieverConnectorTest {

    private static final String BASE_URL = "http://localhost";
    private static final String API_KEY = "TEST_API_KEY";

    private static WireMockServer wireMockServer;
    private static RetrieverConnector connector;

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        connector = RetrieverConnectorFactory.create(BASE_URL + ":" + wireMockServer.port(), API_KEY);
    }

    @AfterAll
    static void tearDown() {
        connector.close();
        wireMockServer.stop();
    }

    private final ArticlesRequest articlesRequest =
            new ArticlesRequest.Builder()
                    .query("foo bar baz")
                    .fromDate(LocalDate.parse("2026-02-01"))
                    .toDate(LocalDate.parse("2026-03-31"))
                    .build();

    @Test
    void constructor_failSafeHttpClientArgIsNull_throws() {
        assertThrows(NullPointerException.class, () ->
                new RetrieverConnector((FailSafeHttpClient) null, BASE_URL, API_KEY));
    }

    @Test
    void constructor_baseUrlArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> {
            try (Client client = ClientBuilder.newClient()) {
                new RetrieverConnector(client, null, API_KEY);
            }
        });
    }

    @Test
    void constructor_baseUrlArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (Client client = ClientBuilder.newClient()) {
                new RetrieverConnector(client, " ", API_KEY);
            }
        });
    }

    @Test
    void constructor_apiKeyArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> {
            try (Client client = ClientBuilder.newClient()) {
                new RetrieverConnector(client, BASE_URL, null);
            }
        });
    }

    @Test
    void constructor_apiKeyArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> {
            try (Client client = ClientBuilder.newClient()) {
                new RetrieverConnector(client, BASE_URL, " ");
            }
        });
    }

    @Test
    void searchArticles_serviceReturnsOk() throws RetrieverConnectorException {
        String responseBody = """
            {
              "total": 3,
              "documents": [
                {
                  "DOC_ID": "foo"
                },
                {
                  "DOC_ID": "bar"
                },
                {
                  "DOC_ID": "baz"
                }
              ]
            }
            """;

        wireMockServer.stubFor(post(urlEqualTo("/articles/search"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        ArticlesResponse result = connector.searchArticles(articlesRequest);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/articles/search"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {
                          "query": "foo bar baz",
                          "fromDate": "2026-02-01",
                          "toDate": "2026-03-31"
                        }
                        """)));

        assertThat("total", result.total(), is(3));
        assertThat("documents",
                result.articles().stream().map(article -> article.get("DOC_ID", String.class)).toList(),
                is(List.of("foo", "bar", "baz")));
    }

    @Test
    void searchArticles_serviceReturnsError() throws RetrieverConnectorException {
        String responseBody = """
            {
              "status": 417,
              "message": "Something went wrong",
              "timestamp": "2026-03-17T00:00:00.000Z"
            }
            """;

        wireMockServer.stubFor(post(urlEqualTo("/articles/search"))
                .willReturn(aResponse()
                        .withStatus(417)
                        .withHeader("Content-Type", "application/json")
                        .withBody(responseBody)));

        var e = assertThrows(RetrieverConnectorUnexpectedStatusCodeException.class, () -> connector.searchArticles(articlesRequest));
        assertThat("message", e.getMessage(),
                is("Retriever service returned with unexpected status code: <Expectation Failed> and message: Something went wrong"));
    }
}
