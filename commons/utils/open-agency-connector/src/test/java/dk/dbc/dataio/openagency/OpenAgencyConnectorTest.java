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

package dk.dbc.dataio.openagency;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.dbc.oss.ns.openagency.Information;
import dk.dbc.oss.ns.openagency.LibraryRules;
import org.junit.Rule;
import org.junit.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

public class OpenAgencyConnectorTest {
    /* See OpenAgencyWireMockRecorder class for info on how to repeat wiremock recordings */

    private static final String WIREMOCK_PORT = System.getProperty("wiremock.port", "8998");
    private static final long AGENCY_ID = 100300;

    @Rule
    public WireMockRule wireMockRule = new WireMockRule(Integer.valueOf(WIREMOCK_PORT));

    String openAgencyEndpoint = String.format("http://localhost:%s/", WIREMOCK_PORT);
    OpenAgencyConnector openAgencyConnector = new OpenAgencyConnector(openAgencyEndpoint);

    @Test(expected = NullPointerException.class)
    public void constructor_endpointArgIsNull_throws() {
        new OpenAgencyConnector(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_endpointArgIsEmpty_throws() {
        new OpenAgencyConnector(" ");
    }

    @Test
    public void getAgencyInformation_agencyIdExists_returnsInformation() throws OpenAgencyConnectorException {
        final Information agencyInformation = getAgencyInformationForExistingAgency();
        assertThat("Information", agencyInformation, is(notNullValue()));
        assertThat("Information.getAgencyName()", agencyInformation.getAgencyName(), is("Integra Testbiblioteker"));
    }

    private Information getAgencyInformationForExistingAgency() throws OpenAgencyConnectorException {
        return openAgencyConnector.getAgencyInformation(AGENCY_ID).orElse(null);
    }

    @Test
    public void getAgencyInformation_agencyIdDoesNotExist_returns() throws OpenAgencyConnectorException {
        final Information agencyInformation = getAgencyInformationForNonExistingAgency();
        assertThat("Information", agencyInformation, is(nullValue()));
    }

    private Information getAgencyInformationForNonExistingAgency() throws OpenAgencyConnectorException {
        return openAgencyConnector.getAgencyInformation(111111).orElse(null);
    }

    void recordServiceRequests() throws OpenAgencyConnectorException {
        getAgencyInformationForExistingAgency();
        getAgencyInformationForNonExistingAgency();
    }

    @Test
    public void getLibraryRules_agencyIdExists_returnsLibraryRules() throws OpenAgencyConnectorException {
        final LibraryRules libraryRules = getLibraryRulesForExistingAgency();
        assertThat("libraryRules", libraryRules, is(notNullValue()));
        assertThat("libraryRules.getAgencyId()", libraryRules.getAgencyId(), is(String.valueOf(AGENCY_ID)));
    }

    private LibraryRules getLibraryRulesForExistingAgency() throws OpenAgencyConnectorException {
        return openAgencyConnector.getLibraryRules(AGENCY_ID, null).orElse(null);
    }

    @Test
    public void getLibraryRules_agencyIdDoesNotExist_returns() throws OpenAgencyConnectorException {
        final LibraryRules libraryRules = getLibraryRulesForNonExistingAgency();
        assertThat("libraryRules", libraryRules, is(nullValue()));
    }

    private LibraryRules getLibraryRulesForNonExistingAgency() throws OpenAgencyConnectorException {
        return openAgencyConnector.getLibraryRules(111111, null).orElse(null);
    }

    void recordLibraryRulesRequests() throws OpenAgencyConnectorException {
        getLibraryRulesForExistingAgency();
        getLibraryRulesForNonExistingAgency();
    }
}