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

package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.AddiMetaData.LibraryRules;
import dk.dbc.dataio.openagency.OpenAgencyConnector;
import dk.dbc.dataio.openagency.OpenAgencyConnectorException;
import dk.dbc.oss.ns.openagency.LibraryRule;
import org.junit.Test;

import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AgencyConnectionTest {
    final OpenAgencyConnector connector = mock(OpenAgencyConnector.class);

    @Test(expected = NullPointerException.class)
    public void constructor_endpointArgIsNull_throws() {
        new AgencyConnection((String) null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_endpointArgIsEmpty_throws() {
        new AgencyConnection(" ");
    }

    @Test
    public void getLibraryRules_connectorReturnsEmptyResponse_returnsNull() throws OpenAgencyConnectorException {
        when(connector.getLibraryRules(anyLong(), anyString())).thenReturn(Optional.empty());
        assertThat(createAgencyConnection().getLibraryRules(123456, null), is(nullValue()));
    }

    @Test
    public void getLibraryRules_connectorReturnsNonEmptyResponse_returnsLibraryRules() throws OpenAgencyConnectorException {
        final LibraryRule firstLibraryRule = new LibraryRule();
        firstLibraryRule.setName("1st rule");
        firstLibraryRule.setBool(true);
        final LibraryRule secondLibraryRule = new LibraryRule();
        secondLibraryRule.setName("2nd rule");
        secondLibraryRule.setBool(false);
        final dk.dbc.oss.ns.openagency.LibraryRules response = new dk.dbc.oss.ns.openagency.LibraryRules();
        response.setAgencyType("myType");
        response.getLibraryRule().add(firstLibraryRule);
        response.getLibraryRule().add(secondLibraryRule);

        final LibraryRules expectedLibraryRules = new LibraryRules()
                .withAgencyType(response.getAgencyType())
                .withLibraryRule(firstLibraryRule.getName(), firstLibraryRule.isBool())
                .withLibraryRule(secondLibraryRule.getName(), secondLibraryRule.isBool());

        when(connector.getLibraryRules(anyLong(), anyString())).thenReturn(Optional.of(response));
        assertThat(createAgencyConnection().getLibraryRules(123456, null), is(expectedLibraryRules));
    }

    private AgencyConnection createAgencyConnection() {
        return new AgencyConnection(connector);
    }
}