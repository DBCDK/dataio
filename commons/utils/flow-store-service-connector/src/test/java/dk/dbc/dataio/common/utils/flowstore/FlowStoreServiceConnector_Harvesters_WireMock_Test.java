package dk.dbc.dataio.common.utils.flowstore;

import com.fasterxml.jackson.databind.type.CollectionType;
import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.useragent.UserAgent;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.rest.FlowStoreServiceConstants;
import dk.dbc.dataio.harvester.types.HarvesterConfig;
import dk.dbc.dataio.harvester.types.HttpPickup;
import dk.dbc.dataio.harvester.types.PeriodicJobsHarvesterConfig;
import dk.dbc.dataio.harvester.types.PeriodicJobsV3HarvesterConfig;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.httpclient.HttpClient;
import dk.dbc.httpclient.PathBuilder;
import jakarta.ws.rs.client.Client;
import jakarta.ws.rs.core.MediaType;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.core.Is.is;

@WireMockTest
public class FlowStoreServiceConnector_Harvesters_WireMock_Test {
    private final JSONBContext jsonbContext = new JSONBContext();
    private String wireMockEndpoint;

    @BeforeEach
    public void init(WireMockRuntimeInfo wireMockRuntimeInfo) {
        wireMockEndpoint = wireMockRuntimeInfo.getHttpBaseUrl();
    }

    @Test
    public void findHarvesterConfigsByTypeForRRType() throws Exception {
        FlowStoreServiceConnector connector = createTestConnector();
        String path = "/" + String.join("/", new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE)
                .bind(FlowStoreServiceConstants.TYPE_VARIABLE, RRHarvesterConfig.class.getName())
                .build());

        stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(marshall(buildRRList()))
                )
        );

        List<RRHarvesterConfig> response = connector.findHarvesterConfigsByType(RRHarvesterConfig.class);

        assertThat(response.size(), is(1));
        assertThat(response.get(0).getId(), is(1L));
        List<RRHarvesterConfig> expectedList = buildRRList();
        assertThat(response.get(0), is(expectedList.get(0)));
    }

    /**
     * The periodic jobs sink resolves the config of any periodic job as a
     * {@link PeriodicJobsHarvesterConfig}, also when the job originates from the
     * rawrepo v3 based harvester, which stores its configs as the
     * {@link PeriodicJobsV3HarvesterConfig} subtype.
     */
    @Test
    void getHarvesterConfigOfV3TypeAsPeriodicJobsType() throws Exception {
        FlowStoreServiceConnector connector = createTestConnector();
        String path = "/" + String.join("/", new PathBuilder(FlowStoreServiceConstants.HARVESTER_CONFIG)
                .bind(FlowStoreServiceConstants.ID_VARIABLE, 20301)
                .build());

        PeriodicJobsV3HarvesterConfig config = new PeriodicJobsV3HarvesterConfig(20301, 1,
                new PeriodicJobsV3HarvesterConfig.Content()
                        .withName("v3 periodic job")
                        .withSubmitterNumber("870970")
                        .withPickup(new HttpPickup().withReceivingAgency("870970")));

        stubFor(get(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(jsonbContext.marshall(config))
                )
        );

        PeriodicJobsHarvesterConfig response = connector.getHarvesterConfig(20301, PeriodicJobsHarvesterConfig.class);

        assertThat("resolved type", response, is(instanceOf(PeriodicJobsV3HarvesterConfig.class)));
        assertThat("config", response, is(config));
        assertThat("pickup", response.getContent().getPickup(), is(config.getContent().getPickup()));
        assertThat("harvester token", response.getHarvesterToken(), is("periodic-jobs:20301:1"));
    }

    private List<RRHarvesterConfig> buildRRList() {
        RRHarvesterConfig rrHarvesterConfig = new RRHarvesterConfig(1, 2,
                new RRHarvesterConfig.Content()
                        .withFormat("format")
                        .withBatchSize(12)
                        .withConsumerId("ConsumerId")
                        .withDestination("Destination")
                        .withIncludeRelations(false)
                        .withResource("Resource")
                        .withType(JobSpecification.Type.TEST)
                        .withFormatOverridesEntry(12, "overwride 12")
                        .withFormatOverridesEntry(191919, "must be 870970")
                        .withId("harvest log id")
                        .withEnabled(true)
        );
        List<RRHarvesterConfig> list = new ArrayList<>();
        list.add(rrHarvesterConfig);
        return list;
    }

    private FlowStoreServiceConnector createTestConnector() {
        Client client = HttpClient.newClient(new ClientConfig().register(new JacksonFeature()));
        return new FlowStoreServiceConnector(client, new UserAgent("test"), wireMockEndpoint);
    }

    private <T extends HarvesterConfig> String marshall(List<T> list) throws JSONBException {
        CollectionType collectionType = jsonbContext.getTypeFactory().constructCollectionType(List.class, HarvesterConfig.class);
        return jsonbContext.marshall(list, collectionType);
    }
}
