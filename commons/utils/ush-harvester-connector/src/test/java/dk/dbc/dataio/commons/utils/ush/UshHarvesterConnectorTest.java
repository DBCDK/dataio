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

package dk.dbc.dataio.commons.utils.ush;

import dk.dbc.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.rest.MockedResponse;
import dk.dbc.dataio.harvester.types.UshHarvesterProperties;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ws.rs.client.Client;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Date;
import java.util.List;
import java.util.Map;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.when;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class
})
public class UshHarvesterConnectorTest {
    private static final Client CLIENT = mock(Client.class);
    private static final String USH_HARVESTER_URL = "http://dataio/ush/harvester";

    private final UshHarvesterProperties expectedUshHarvesterProperties = getExpectedUshHarvesterProperties();
    private final String ushHarvesterJobXml = readTestRecordAsString("/ush-harvester-job.xml");
    private static final int ID = 10002;
    private static final Date Tue_May_31_00_00_05_CEST_2016 = new Date(1464645605419L); //lastHarvestFinished
    private static final Date Tue_May_31_00_00_00_CEST_2016 = new Date(1464645600239L); //lastHarvestStarted
    private static final Date Mon_May_23_13_13_32_CEST_2016 = new Date(1464002012515L); //lastUpdated
    private static final Date Mon_Jun_06_02_00_00_CEST_2016 = new Date(1465171200000L); //nextHarvestSchedule

    private UshHarvesterConnector connector;

    @Before
    public void setup() throws Exception {
        mockStatic(HttpClient.class);
        connector = newUshHarvesterConnector();
    }

    @Test(expected = NullPointerException.class)
    public void constructor_httpClientArgIsNull_throws() {
        new UshHarvesterConnector(null, USH_HARVESTER_URL);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_baseUrlArgIsNull_throws() {
        new UshHarvesterConnector(CLIENT, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_baseUrlArgIsEmpty_throws() {
        new UshHarvesterConnector(CLIENT, "");
    }

    @Test
    public void constructor_allArgsAreValid_returnsNewInstance() {
        final UshHarvesterConnector instance = newUshHarvesterConnector();
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getHttpClient(), is(CLIENT));
        assertThat(instance.getBaseUrl(), is(USH_HARVESTER_URL));
    }

    @Test
    public void listUshHarvesterJobs_serviceReturnsValidXmlContainingJobs_returnsNoneEmptyList() throws UshHarvesterConnectorException {
        // Setup
        setupUshHarvesterMockedHttpResponse(Response.Status.OK, ushHarvesterJobXml);

        // Subject under test
        final List<UshHarvesterProperties> ushHarvesterPropertiesList = connector.listUshHarvesterJobs();

        // Verification

        assertThat(ushHarvesterPropertiesList.size(), is(1));
        assertThat(ushHarvesterPropertiesList.get(0), is(expectedUshHarvesterProperties));
    }

    @Test
    public void listUshHarvesterJobs_serviceReturnsValidXmlContainingZeroJobs_returnsEmptyList() throws UshHarvesterConnectorException {
        // Setup
        setupUshHarvesterMockedHttpResponse(Response.Status.OK, "<harvestables/>");

        // Subject under test
        final List<UshHarvesterProperties> ushHarvesterProperties = connector.listUshHarvesterJobs();

        // Verification
        assertThat(ushHarvesterProperties.isEmpty(), is(true));
    }

    @Test
    public void listUshHarvesterJobs_serviceReturnsInvalidXml_throws() throws UshHarvesterConnectorException {
        setupUshHarvesterMockedHttpResponse(Response.Status.OK, "invalidXml");
        assertThat(() -> connector.listUshHarvesterJobs(), isThrowing(UshHarvesterConnectorException.class));
    }

    @Test
    public void listUshHarvesterJobs_serviceReturnsNullEntity_throws() throws UshHarvesterConnectorException {
        setupUshHarvesterMockedHttpResponse(Response.Status.OK, null);
        assertThat(() -> connector.listUshHarvesterJobs(), isThrowing(UshHarvesterConnectorException.class));
    }

    @Test
    public void listUshHarvesterJobs_serviceReturnsInternalServerError_throws() throws UshHarvesterConnectorException {
        // Setup
        setupUshHarvesterMockedHttpResponse(Response.Status.INTERNAL_SERVER_ERROR, null);
        try {
            // Subject under test
            connector.listUshHarvesterJobs();
            fail("No exception thrown");
        } catch (UshHarvesterConnectorUnexpectedStatusCodeException e) {
            assertThat(e.getStatusCode(), is(500));
        }
    }

    @Test
    public void listIndexedUshHarvesterJobs_serviceReturnsValidXmlContainingJobs_returnsNoneEmptyMap() throws UshHarvesterConnectorException {
        // Setup
        setupUshHarvesterMockedHttpResponse(Response.Status.OK, ushHarvesterJobXml);

        // Subject under test
        Map<Integer, UshHarvesterProperties> indexedUshHarvesterJobs = connector.listIndexedUshHarvesterJobs();

        // Verification
        assertThat(indexedUshHarvesterJobs.containsKey(ID), is(true));
        assertThat(indexedUshHarvesterJobs.get(ID), is(expectedUshHarvesterProperties));

    }

    @Test
    public void listIndexedUshHarvesterJobs_serviceReturnsValidXmlContainingZeroJobs_returnsEmptyMap() throws UshHarvesterConnectorException {
        // Setup
        setupUshHarvesterMockedHttpResponse(Response.Status.OK, "<harvestables/>");

        // Subject under test
        Map<Integer, UshHarvesterProperties> indexedUshHarvesterJobs = connector.listIndexedUshHarvesterJobs();

        // Verification
        assertThat(indexedUshHarvesterJobs.isEmpty(), is(true));
    }


    /*
     * Private methods
     */

    private void setupUshHarvesterMockedHttpResponse(Response.Status statusCode, Object returnValue) {
        when(HttpClient.doGet(CLIENT, USH_HARVESTER_URL, "records", "harvestables"))
                .thenReturn(new MockedResponse<>(statusCode.getStatusCode(), returnValue));
    }

    private UshHarvesterConnector newUshHarvesterConnector() {
        try {
            return new UshHarvesterConnector(CLIENT, USH_HARVESTER_URL);
        } catch (Exception e) {
            fail("Caught unexpected exception " + e.getClass().getCanonicalName() + ": " + e.getMessage());
            throw new IllegalStateException(e);
        }
    }

    private String readTestRecordAsString(String resourceName) {
        return StringUtil.asString(readTestRecord(resourceName), StandardCharsets.UTF_8);
    }

    private byte[] readTestRecord(String resourceName) {
        try {
            return Files.readAllBytes(Paths.get(getClass().getResource(resourceName).toURI()));
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }

    private UshHarvesterProperties getExpectedUshHarvesterProperties() {
        return new UshHarvesterProperties()
                .withUri("http://dataio-ush-s01.dbc.dk:8080/harvester/records/harvestables/10002/")
                .withAmountHarvested(25)
                .withCurrentStatus("OK")
                .withEnabled(true)
                .withId(10002)
                .withJobClass("OaiPmhResource")
                .withLastHarvestFinishedDate(Tue_May_31_00_00_05_CEST_2016)
                .withLastHarvestStartedDate(Tue_May_31_00_00_00_CEST_2016)
                .withLastUpdatedDate(Mon_May_23_13_13_32_CEST_2016)
                .withMessage("Stop requested after 25 records")
                .withName("KB 810010 test")
                .withNextHarvestSchedule(Mon_Jun_06_02_00_00_CEST_2016)
                .withStorageUrl("http://dataio-ush-s01.dbc.dk:8080/solr4/");
    }
}
