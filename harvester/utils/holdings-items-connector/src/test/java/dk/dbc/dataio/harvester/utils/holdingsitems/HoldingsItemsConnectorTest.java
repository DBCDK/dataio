package dk.dbc.dataio.harvester.utils.holdingsitems;

import com.github.tomakehurst.wiremock.junit5.WireMockRuntimeInfo;
import com.github.tomakehurst.wiremock.junit5.WireMockTest;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

@WireMockTest
public class HoldingsItemsConnectorTest {
    /*
        Steps to reproduce wiremock recording:

        * Start standalone runner
            java -jar wiremock-1.57-standalone.jar --proxy-all="http://cisterne-solr.dbc.dk:8014/solr/beholdplus" --record-mappings --verbose

        * Record resultset for hasHoldings query without hits
            curl "http://localhost:8080/select?q=holdingsitem.bibliographicRecordId%3Ano-such-record-id&group=true&group.main=true&group.field=holdingsitem.agencyId&fl=holdingsitem.agencyId&rows=9999&wt=javabin&version=2"

        * Record resultset for hasHoldings query without agencyIds filter
            curl "http://localhost:8080/select?q=holdingsitem.bibliographicRecordId%3A23181444&group=true&group.main=true&group.field=holdingsitem.agencyId&fl=holdingsitem.agencyId&rows=9999&wt=javabin&version=2"

        * Record resultset for hasHoldings query with agencyIds filter
            curl "http://localhost:8998/select?q=holdingsitem.bibliographicRecordId%3A23181444+AND+holdingsitem.agencyId%3A%28771000+OR+111111+OR+767100%29&group=true&group.main=true&group.field=holdingsitem.agencyId&fl=holdingsitem.agencyId&rows=3&wt=javabin&version=2"
     */

    private static String solrServerEndpoint;
    private final String bibliographicRecordId = "23181444";

    @BeforeAll
    public static void init(WireMockRuntimeInfo wireMockRuntimeInfo) {
        solrServerEndpoint = wireMockRuntimeInfo.getHttpBaseUrl();
    }

    @Test
    public void constructor_solrServerEndpointArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new HoldingsItemsConnector(null));
    }

    @Test
    public void constructor_solrServerEndpointArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new HoldingsItemsConnector(" "));
    }

    @Test
    public void hasHoldings_bibliographicRecordIdArgIsNull_throws() {
        HoldingsItemsConnector connector = createConnector();
        assertThat(() -> connector.hasHoldings(null, Collections.emptySet()), isThrowing(NullPointerException.class));
    }

    @Test
    public void hasHoldings_bibliographicRecordIdArgIsEmpty_throws() {
        HoldingsItemsConnector connector = createConnector();
        assertThat(() -> connector.hasHoldings(" ", Collections.emptySet()), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void hasHoldings_agencyIdsArgIsNull_throws() {
        HoldingsItemsConnector connector = createConnector();
        assertThat(() -> connector.hasHoldings(bibliographicRecordId, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void hasHoldings_noHoldingsFound_returnsEmptySet() {
        HoldingsItemsConnector connector = createConnector();
        assertThat(connector.hasHoldings("no-such-record-id", Collections.emptySet()), is(Collections.emptySet()));
    }

    @Test
    public void hasHoldings_agencyIdsArgIsEmpty_returnsSetOfAllAgencyIdsWithHoldings() {
        HoldingsItemsConnector connector = createConnector();
        assertThat(connector.hasHoldings(bibliographicRecordId, Collections.emptySet()), is(toSet(300320, 718700, 737600, 767100, 770700, 771000)));
    }

    @Test
    public void hasHoldings_agencyIdsArgIsNonEmpty_returnsSetOfAgencyIdsWithHoldings() {
        HoldingsItemsConnector connector = createConnector();
        assertThat(connector.hasHoldings(bibliographicRecordId, toSet(111111, 767100, 771000)), is(toSet(767100, 771000)));
    }

    private HoldingsItemsConnector createConnector() {
        return new HoldingsItemsConnector(solrServerEndpoint, "HoldingsItemsConnectorTest");
    }

    private Set<Integer> toSet(Integer... ints) {
        return Stream.of(ints).collect(Collectors.toSet());
    }
}
