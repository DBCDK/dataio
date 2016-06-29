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

package dk.dbc.dataio.commons.utils.ush.solr;


import com.fasterxml.jackson.jaxrs.json.JacksonJaxbJsonProvider;
import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.dbc.dataio.commons.types.ServiceError;
import dk.dbc.dataio.commons.types.rest.UshServiceConstants;
import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.httpclient.PathBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.MediaType;

import static com.github.tomakehurst.wiremock.client.WireMock.aResponse;
import static com.github.tomakehurst.wiremock.client.WireMock.post;
import static com.github.tomakehurst.wiremock.client.WireMock.stubFor;
import static com.github.tomakehurst.wiremock.client.WireMock.urlEqualTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;

public class UshSolrHarvesterWireMockTest {

    private String baseUrl;
    private Client client;

    private final static int JOB_ID = 42;
    private final static int USH_SOLR_HARVESTER_CONFIG_ID = 123;
    private final JSONBContext jsonbContext = new JSONBContext();

    @Rule  // Port 0 lets wiremock find a random port
    public WireMockRule wireMockRule = new WireMockRule(0);

    @Before
    public void setup() {
        baseUrl = String.format("http://localhost:%d/", wireMockRule.port());
        client = HttpClient.newClient();
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new UshSolrHarvesterConnector(null, baseUrl);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new UshSolrHarvesterConnector(client, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new UshSolrHarvesterConnector(client, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final UshSolrHarvesterConnector instance = createTestConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(client));
        assertThat(instance.getBaseUrl(), is(baseUrl));
    }


    @Test
    public void runTestHarvest_serviceExecutesSuccessfulTestHarvest_returnsJobIdLocation() throws Exception {
        final UshSolrHarvesterConnector connector = createTestConnector();
        final String path = "/" + String.join("/", (CharSequence[]) new PathBuilder(UshServiceConstants.HARVESTERS_USH_SOLR_TEST)
                .bind(UshServiceConstants.ID_VARIABLE, Long.toString(USH_SOLR_HARVESTER_CONFIG_ID))
                .build());

        stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(201)
                        .withHeader("Location", Long.valueOf(JOB_ID).toString()))
        );

        final int response = connector.runTestHarvest(USH_SOLR_HARVESTER_CONFIG_ID);
        assertThat(response, is(JOB_ID));
    }

    @Test
    public void runTestHarvest_responseWithInternalServerError_throws() throws Exception {
        final UshSolrHarvesterConnector connector = createTestConnector();
        final String path = "/" + String.join("/", (CharSequence[]) new PathBuilder(UshServiceConstants.HARVESTERS_USH_SOLR_TEST)
                .bind(UshServiceConstants.ID_VARIABLE, Long.toString(USH_SOLR_HARVESTER_CONFIG_ID))
                .build());

        stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(500)
                        .withHeader("Content-Type", MediaType.APPLICATION_JSON)
                        .withBody(jsonbContext.marshall(new ServiceError()))
                )
        );

        try {
            connector.runTestHarvest(USH_SOLR_HARVESTER_CONFIG_ID);
        } catch (UshSolrHarvesterConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getServiceError(), is(notNullValue()));
        }
    }

    @Test
    public void runTestHarvest_responseWithNoContent_throws() throws Exception {
        final UshSolrHarvesterConnector connector = createTestConnector();
        final String path = "/" + String.join("/", (CharSequence[]) new PathBuilder(UshServiceConstants.HARVESTERS_USH_SOLR_TEST)
                .bind(UshServiceConstants.ID_VARIABLE, Long.toString(USH_SOLR_HARVESTER_CONFIG_ID))
                .build());

        stubFor(post(urlEqualTo(path))
                .willReturn(aResponse()
                        .withStatus(204)
                )
        );

        try {
            connector.runTestHarvest(USH_SOLR_HARVESTER_CONFIG_ID);
        } catch (UshSolrHarvesterConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getServiceError(), is(nullValue()));
        }
    }

    /*
     * Private methods
     */

    private UshSolrHarvesterConnector createTestConnector() {
        final ClientConfig cfg = new ClientConfig();
        cfg.register(JacksonJsonProvider.class);
        cfg.register(JacksonJaxbJsonProvider.class);
        return new UshSolrHarvesterConnector(client, baseUrl);
    }

}
