package dk.dbc.dataio.sink.rawrepo.update.v3.connector;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.tomakehurst.wiremock.WireMockServer;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.httpclient.FailSafeHttpClient;
import dk.dbc.httpclient.HttpClient;
import jakarta.ws.rs.core.Response;
import net.jodah.failsafe.RetryPolicy;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.equalTo;
import static com.github.tomakehurst.wiremock.client.WireMock.equalToJson;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.postRequestedFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static com.github.tomakehurst.wiremock.core.WireMockConfiguration.wireMockConfig;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

class UpdateServiceConnectorTest {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private static final JsonNode MARC_RECORD = json("""
            {
              "fields": [
                {
                  "name": "001",
                  "indicator": ["0", "0"],
                  "subfields": [
                    { "name": "a", "value": "135010529" },
                    { "name": "b", "value": "870970" }
                  ]
                }
              ]
            }
            """);

    private static WireMockServer wireMockServer;
    private static UpdateServiceConnector connector;

    @BeforeAll
    static void setUp() {
        wireMockServer = new WireMockServer(wireMockConfig().dynamicPort());
        wireMockServer.start();
        FailSafeHttpClient failSafeHttpClient = FailSafeHttpClient.create(
                HttpClient.newClient(new ClientConfig().register(new JacksonFeature())),
                new UserAgent(UpdateServiceConnectorTest.class.getSimpleName()),
                new RetryPolicy<Response>().withMaxRetries(0));
        connector = new UpdateServiceConnector(failSafeHttpClient, "http://localhost:" + wireMockServer.port());
    }

    @AfterAll
    static void tearDown() {
        connector.close();
        wireMockServer.stop();
    }

    @BeforeEach
    void resetWireMock() {
        wireMockServer.resetAll();
    }

    private static JsonNode json(String json) {
        try {
            return OBJECT_MAPPER.readTree(json);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private UpdateRequest buildRequest(String type, String templateName, JsonNode content) {
        Authentication auth = new Authentication();
        auth.setUserId("user");
        auth.setGroupId("870970");
        auth.setPassword("secret");

        UpdateRequest request = new UpdateRequest();
        request.setType(type);
        request.setSubmitter("870970");
        request.setAuthentication(auth);
        request.setTemplateName(templateName);
        request.setContent(content);
        return request;
    }

    @Test
    void update_typeAbsent_defaultsToDbcPath() throws UpdateServiceConnectorException {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\"}")));

        UpdateRequest request = new UpdateRequest();  // type not set — default "dbc" applies
        request.setAuthentication(new Authentication());
        request.setTemplateName("bog");
        request.setContent(MARC_RECORD);
        UpdateResponse response = connector.update(request);

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/update/dbc")));
        assertThat(response.getStatus(), is(UpdateResponseStatus.OK));
    }

    @Test
    void update_typeDbc_sendsToV2DbcPath() throws UpdateServiceConnectorException {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\"}")));

        UpdateResponse response = connector.update(buildRequest("dbc", "bog", MARC_RECORD));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/update/dbc")));
        assertThat(response.getStatus(), is(UpdateResponseStatus.OK));
    }

    @Test
    void update_requestBodyContainsAuthTemplateAndContent_typeAndSubmitterNotInBody() throws UpdateServiceConnectorException {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\"}")));

        connector.update(buildRequest("dbc", "bog", MARC_RECORD));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/update/dbc"))
                .withHeader("Content-Type", equalTo("application/json"))
                .withRequestBody(equalToJson("""
                        {
                            "authentication": {
                                "userId": "user",
                                "groupId": "870970",
                                "password": "secret"
                            },
                            "templateName": "bog",
                            "content": {
                              "fields": [
                                {
                                  "name": "001",
                                  "indicator": ["0", "0"],
                                  "subfields": [
                                    { "name": "a", "value": "135010529" },
                                    { "name": "b", "value": "870970" }
                                  ]
                                }
                              ]
                            }
                        }
                        """, true, true)));
    }

    @Test
    void update_http200WithStatusOk_returnsOkResponse() throws UpdateServiceConnectorException {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\"}")));

        UpdateResponse response = connector.update(buildRequest("dbc", "bog", MARC_RECORD));

        assertThat(response.getStatus(), is(UpdateResponseStatus.OK));
    }

    @Test
    void update_http400WithErrorsArray_returnsErrorsPopulated() throws UpdateServiceConnectorException {
        String body = """
                {
                    "status": "ERROR",
                    "errors": [
                        {
                            "type": "ERROR",
                            "message": "Felt 245 delfelt a mangler",
                            "ordinalPositionOfField": 3,
                            "ordinalPositionOfSubfield": 1
                        }
                    ]
                }
                """;
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc"))
                .willReturn(aResponse()
                        .withStatus(400)
                        .withHeader("Content-Type", "application/json")
                        .withBody(body)));

        UpdateResponse response = connector.update(buildRequest("dbc", "bog", MARC_RECORD));

        assertThat(response.getStatus(), is(UpdateResponseStatus.ERROR));
        assertThat(response.getErrors(), notNullValue());
        assertThat(response.getErrors().size(), is(1));
        assertThat(response.getErrors().get(0).getMessage(), is("Felt 245 delfelt a mangler"));
        assertThat(response.getErrors().get(0).getOrdinalPositionOfField(), is(3));
        assertThat(response.getErrors().get(0).getOrdinalPositionOfSubfield(), is(1));
    }

    @Test
    void update_responseMissingStatusField_throwsException() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"errors\":[]}")));

        assertThrows(UpdateServiceConnectorException.class,
                () -> connector.update(buildRequest("dbc", "bog", MARC_RECORD)));
    }

    @Test
    void update_http401_throwsUnexpectedStatusCodeException() {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc"))
                .willReturn(aResponse().withStatus(401)));

        UpdateServiceConnectorUnexpectedStatusCodeException exception = assertThrows(
                UpdateServiceConnectorUnexpectedStatusCodeException.class,
                () -> connector.update(buildRequest("dbc", "bog", MARC_RECORD)));
        assertThat(exception.getStatusCode(), is(401));
    }

    @Test
    void validate_typeDbc_callsV2ValidatePath() throws UpdateServiceConnectorException {
        wireMockServer.stubFor(post(urlEqualTo("/api/v2/update/dbc/validate"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", "application/json")
                        .withBody("{\"status\":\"OK\"}")));

        UpdateResponse response = connector.validate(buildRequest("dbc", "bog", MARC_RECORD));

        wireMockServer.verify(postRequestedFor(urlEqualTo("/api/v2/update/dbc/validate")));
        assertThat(response.getStatus(), is(UpdateResponseStatus.OK));
    }
}
