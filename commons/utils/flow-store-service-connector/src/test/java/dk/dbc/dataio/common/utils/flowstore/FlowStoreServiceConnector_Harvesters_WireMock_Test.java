package dk.dbc.dataio.common.utils.flowstore;

import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.get;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig;
import dk.dbc.dataio.harvester.types.OpenAgencyTarget;
import dk.dbc.dataio.harvester.types.RRHarvesterConfig;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.glassfish.jersey.client.ClientConfig;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import org.junit.After;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by ja7 on 20-05-16.
 */


public class FlowStoreServiceConnector_Harvesters_WireMock_Test {

    private static final int WIREMOCK_PORT = Integer.valueOf(System.getProperty("wiremock.port", "8998"));
    private final JSONBContext jsonbContext = new JSONBContext();

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(WIREMOCK_PORT);
    private final String baseURL = "http://localhost:" + WIREMOCK_PORT + "/";


    @Before
    public void setUp() throws Exception {

    }

    @After
    public void tearDown() throws Exception {

    }

    @Test
    public void findHarvesterConfigsByTypeForOldRRType() throws Exception {

        FlowStoreServiceConnector connector = createTestConnector();
        // TODO: use FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE
        stubFor(get(urlEqualTo("/harvester-configs/types/dk.dbc.dataio.harvester.types.OLDRRHarvesterConfig"))
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(jsonbContext.marshall(buildOLDRRList()))
                )
        );

        List<OLDRRHarvesterConfig> res = connector.findHarvesterConfigsByType(OLDRRHarvesterConfig.class);

        assertThat(res.size(), is(1));
        assertThat(res.get(0).getId(),is( 1L ));
        List<OLDRRHarvesterConfig> r=buildOLDRRList();
        assertThat(res.get(0),is( r.get(0)) );
    }


    @Test
    public void findHarvesterConfigsByTypeForRRType() throws Exception {

        FlowStoreServiceConnector connector = createTestConnector();
        // TODO: use FlowStoreServiceConstants.HARVESTER_CONFIGS_TYPE
        stubFor(get(urlEqualTo("/harvester-configs/types/dk.dbc.dataio.harvester.types.RRHarvesterConfig"))
                //.withHeader("Accept", equalTo(MediaType.APPLICATION_JSON) )
                .willReturn(aResponse()
                        .withStatus(200)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(jsonbContext.marshall(buildRRList()))
                )
        );

        List<RRHarvesterConfig> res = connector.findHarvesterConfigsByType(RRHarvesterConfig.class);

        assertThat(res.size(), is(1));
        assertThat(res.get(0).getId(),is( 1L ));
        List<RRHarvesterConfig> r=buildRRList();
        assertThat(res.get(0),is( r.get(0)) );


    }

    private List<OLDRRHarvesterConfig> buildOLDRRList() {

        OLDRRHarvesterConfig rrHarvesterConfig = new OLDRRHarvesterConfig(1, 2,
                new OLDRRHarvesterConfig.Content()
                        .wihtFormat("format")
                        .withBatchSize(12)
                        .withConsumerId("ConsumerId")
                        .withDestination("Destination")
                        .withIncludeRelations(false)
                        .withOpenAgencyTarget(new OpenAgencyTarget())
                        .withResource("Resource")
                        .withType(JobSpecification.Type.ACCTEST)
                        .withFormatOverridesEntry(12, "overwride 12")
                        .withFormatOverridesEntry(191919, "must be 870970")
                        .withId("harvest log id")
                        .withEnabled(true)
        );
        List<OLDRRHarvesterConfig> lres = new ArrayList<>();
        lres.add(rrHarvesterConfig);
        return lres;
    }

    private List<RRHarvesterConfig> buildRRList() {

        RRHarvesterConfig rrHarvesterConfig = new RRHarvesterConfig(1, 2,
                new RRHarvesterConfig.Content()
                        .wihtFormat("format")
                        .withBatchSize(12)
                        .withConsumerId("ConsumerId")
                        .withDestination("Destination")
                        .withIncludeRelations(false)
                        .withOpenAgencyTarget(new OpenAgencyTarget())
                        .withResource("Resource")
                        .withType(JobSpecification.Type.ACCTEST)
                        .withFormatOverridesEntry(12, "overwride 12")
                        .withFormatOverridesEntry(191919, "must be 870970")
                        .withId("harvest log id")
                        .withEnabled(true)
        );
        List<RRHarvesterConfig> lres = new ArrayList<>();
        lres.add(rrHarvesterConfig);
        return lres;

    }


    private FlowStoreServiceConnector createTestConnector() {
        ClientConfig cfg = new ClientConfig();
        cfg.register(JacksonJsonProvider.class);
        cfg.register(JacksonJaxbJsonProvider.class);
        Client client = HttpClient.newClient();

        return new FlowStoreServiceConnector(client, baseURL);
    }

}