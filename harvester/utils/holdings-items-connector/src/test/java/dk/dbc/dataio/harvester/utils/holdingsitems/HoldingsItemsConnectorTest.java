/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.harvester.utils.holdingsitems;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Rule;
import org.junit.Test;
import org.junit.contrib.java.lang.system.EnvironmentVariables;

import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

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

    private static final String WIREMOCK_PORT = System.getProperty("wiremock.port", "8998");

    @ClassRule
    public static final EnvironmentVariables ENVIRONMENT_VARIABLES = new EnvironmentVariables();

    @BeforeClass
    public static void setupEnvironment() {
        ENVIRONMENT_VARIABLES.set("SOLR_APPID", "HoldingsItemsConnectorTest");
    }

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(Integer.valueOf(WIREMOCK_PORT));

    private final String solrServerEndpoint = String.format("http://localhost:%s/", WIREMOCK_PORT);
    private final String bibliographicRecordId = "23181444";

    @Test(expected = NullPointerException.class)
    public void constructor_solrServerEndpointArgIsNull_throws() {
        new HoldingsItemsConnector(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_solrServerEndpointArgIsEmpty_throws() {
        new HoldingsItemsConnector(" ");
    }

    @Test
    public void hasHoldings_bibliographicRecordIdArgIsNull_throws() {
        final HoldingsItemsConnector connector = createConnector();
        assertThat(() -> connector.hasHoldings(null, Collections.emptySet()), isThrowing(NullPointerException.class));
    }

    @Test
    public void hasHoldings_bibliographicRecordIdArgIsEmpty_throws() {
        final HoldingsItemsConnector connector = createConnector();
        assertThat(() -> connector.hasHoldings(" ", Collections.emptySet()), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void hasHoldings_agencyIdsArgIsNull_throws() {
        final HoldingsItemsConnector connector = createConnector();
        assertThat(() -> connector.hasHoldings(bibliographicRecordId, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void hasHoldings_noHoldingsFound_returnsEmptySet() {
        final HoldingsItemsConnector connector = createConnector();
        assertThat(connector.hasHoldings("no-such-record-id", Collections.emptySet()), is(Collections.emptySet()));
    }

    @Test
    public void hasHoldings_agencyIdsArgIsEmpty_returnsSetOfAllAgencyIdsWithHoldings() {
        final HoldingsItemsConnector connector = createConnector();
        assertThat(connector.hasHoldings(bibliographicRecordId, Collections.emptySet()), is(toSet(300320, 718700, 737600, 767100, 770700, 771000)));
    }

    @Test
    public void hasHoldings_agencyIdsArgIsNonEmpty_returnsSetOfAgencyIdsWithHoldings() {
        final HoldingsItemsConnector connector = createConnector();
        assertThat(connector.hasHoldings(bibliographicRecordId, toSet(111111, 767100, 771000)), is(toSet(767100, 771000)));
    }

    private HoldingsItemsConnector createConnector() {
        return new HoldingsItemsConnector(solrServerEndpoint);
    }

    private Set<Integer> toSet(Integer... ints) {
        return Stream.of(ints).collect(Collectors.toSet());
    }
}
