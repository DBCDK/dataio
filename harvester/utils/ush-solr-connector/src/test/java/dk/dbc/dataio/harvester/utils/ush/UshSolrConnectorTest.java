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

package dk.dbc.dataio.harvester.utils.ush;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import org.junit.Rule;
import org.junit.Test;

import java.util.Date;
import java.util.Iterator;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertThat;

public class UshSolrConnectorTest {
    /*
        Steps to reproduce wiremock recording:

        * Start standalone runner
            java -jar wiremock-1.57-standalone.jar --proxy-all="http://dataio-ush-s01:8080/solr4/collection1" --record-mappings --verbose

        * Record resultset size for query without hits
           curl "http://localhost:8080/select?q=database%3Ano-such-database+AND+harvest-timestamp%3A%7B*+TO+*%5D&rows=0&wt=javabin&version=2"

        * Record first document batch fetch
           curl "http://localhost:8080/select?q=database%3Ano-such-database+AND+harvest-timestamp%3A%7B*+TO+*%5D&rows=10&start=0&wt=javabin&version=2"

        * Record resultset size for query with hits
            curl "http://localhost:8080/select?q=database%3A10002+AND+harvest-timestamp%3A%7B*+TO+2016-05-25T06%3A59%3A13.740Z%5D&rows=0&wt=javabin&version=2"

        * Record first document batch fetch
            curl "http://localhost:8080/select?q=database%3A10002+AND+harvest-timestamp%3A%7B*+TO+2016-05-25T06%3A59%3A13.740Z%5D&rows=10&start=0&wt=javabin&version=2"

        * Record second document batch fetch
            curl "http://localhost:8080/select?q=database%3A10002+AND+harvest-timestamp%3A%7B*+TO+2016-05-25T06%3A59%3A13.740Z%5D&rows=10&start=10&wt=javabin&version=2"
     */

    private static final String WIREMOCK_PORT = System.getProperty("wiremock.port", "8998");

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(Integer.valueOf(WIREMOCK_PORT));

    private final String solrServerEndpoint = String.format("http://localhost:%s/", WIREMOCK_PORT);
    private final String database = "10002";
    private final Date until = new Date(1464159553740L);  // 2016-05-25T06:59:13.740Z

    /* Kept as a reminder of how to get some crude wiremock debug information...

    @Before
    public void debugWireMock() {
      wireMockRule.addMockServiceRequestListener((request, response) -> {
         System.out.println("URL Requested => " + request.getAbsoluteUrl());
         System.out.println("Request Body => " + request.getBodyAsString());
         System.out.println("Request Headers => " + request.getAllHeaderKeys());
         System.out.println("Response Status => " + response.getStatus());
         System.out.println("Response Body => " + response.getBodyAsString());
      });
   }
   */

    @Test(expected = NullPointerException.class)
    public void constructor_solrServerEndpointArgIsNull_throws() {
        new UshSolrConnector(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_solrServerEndpointArgIsEmpty_throws() {
        new UshSolrConnector(" ");
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor2Arg_documentBufferMaxSizeArgIsLessThanOne_throws() {
        new UshSolrConnector(solrServerEndpoint, 0);
    }

    @Test
    public void findDatabaseDocumentsHarvestedInInterval_databaseArgIsNull_throws() {
        final UshSolrConnector ushSolrConnector = newConnector();
        assertThat(() -> ushSolrConnector.findDatabaseDocumentsHarvestedInInterval(null, null, null), isThrowing(NullPointerException.class));
    }

    @Test
    public void findDatabaseDocumentsHarvestedInInterval_databaseArgIsEmpty_throws() {
        final UshSolrConnector ushSolrConnector = newConnector();
        assertThat(() -> ushSolrConnector.findDatabaseDocumentsHarvestedInInterval(" ", null, null), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void findDatabaseDocumentsHarvestedInInterval_queryFindsNoHits_returnsEmptyResultSet() {
        final UshSolrConnector ushSolrConnector = newConnector();
        final UshSolrConnector.ResultSet resultSet = ushSolrConnector.findDatabaseDocumentsHarvestedInInterval("no-such-database", null, null);
        assertThat("ResultSet size", resultSet.getSize(), is(0));
        final Iterator<UshSolrDocument> iterator = resultSet.iterator();
        assertThat("ResultSet iterator.hasNext()", iterator.hasNext(), is(false));
        assertThat("ResultSet iterator.next()", iterator.next(), is(nullValue()));
    }

    @Test
    public void findDatabaseDocumentsHarvestedInInterval_queryFindsHits_returnsResultSetAbleToRefillDocumentBuffer() {
        final UshSolrConnector ushSolrConnector = newConnector();
        final UshSolrConnector.ResultSet resultSet = ushSolrConnector.findDatabaseDocumentsHarvestedInInterval(database, null, until);
        assertThat("ResultSet size", resultSet.getSize(), is(61315));
        int docNo = 0;
        for (UshSolrDocument ushSolrDocument : resultSet) {
            docNo++;
            if (docNo == 20) {  // We get 10 documents from each buffer (re)fill
                break;
            }
        }
    }

    private UshSolrConnector newConnector() {
        return new UshSolrConnector(solrServerEndpoint, 10);
    }
}