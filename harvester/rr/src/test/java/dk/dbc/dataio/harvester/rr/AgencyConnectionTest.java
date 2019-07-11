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
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.dataio.openagency.OpenAgencyConnector;
import dk.dbc.dataio.openagency.OpenAgencyConnectorException;
import dk.dbc.oss.ns.openagency.LibraryRule;
import org.junit.Test;

import java.util.Optional;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AgencyConnectionTest {
    private final OpenAgencyConnector connector = mock(OpenAgencyConnector.class);

    @Test
    public void getLibraryRules_connectorReturnsEmptyResponse_returnsNull()
            throws OpenAgencyConnectorException {
        when(connector.getLibraryRules(anyLong(), anyString()))
                .thenReturn(Optional.empty());
        assertThat(newAgencyConnection().getLibraryRules(123456, null),
                is(nullValue()));
    }

    @Test
    public void getLibraryRules_connectorReturnsNonEmptyResponse_returnsLibraryRules()
            throws OpenAgencyConnectorException {
        final LibraryRule firstLibraryRule = new LibraryRule();
        firstLibraryRule.setName("1st rule");
        firstLibraryRule.setBool(true);
        final LibraryRule secondLibraryRule = new LibraryRule();
        secondLibraryRule.setName("2nd rule");
        secondLibraryRule.setBool(false);
        final LibraryRule thirdLibraryRule = new LibraryRule();
        thirdLibraryRule.setName("3rd rule");
        thirdLibraryRule.setString("value");
        final dk.dbc.oss.ns.openagency.LibraryRules response = new dk.dbc.oss.ns.openagency.LibraryRules();
        response.setAgencyType("myType");
        response.getLibraryRule().add(firstLibraryRule);
        response.getLibraryRule().add(secondLibraryRule);
        response.getLibraryRule().add(thirdLibraryRule);

        final LibraryRules expectedLibraryRules = new LibraryRules()
                .withAgencyType(response.getAgencyType())
                .withLibraryRule(firstLibraryRule.getName(), firstLibraryRule.isBool())
                .withLibraryRule(secondLibraryRule.getName(), secondLibraryRule.isBool())
                .withLibraryRule(thirdLibraryRule.getName(), thirdLibraryRule.getString());

        when(connector.getLibraryRules(anyLong(), anyString()))
                .thenReturn(Optional.of(response));
        assertThat(newAgencyConnection().getLibraryRules(123456, "test"),
                is(expectedLibraryRules));
    }

    @Test
    public void getLibraryRules_connectorThrows_throws()
            throws OpenAgencyConnectorException {
        when(connector.getLibraryRules(anyLong(), anyString()))
                .thenThrow(new OpenAgencyConnectorException("died"));
        assertThat(() -> newAgencyConnection().getLibraryRules(123456, "test"),
                isThrowing(IllegalStateException.class));
    }

    @Test
    public void getLibraryRules_connectorReturnsNonEmptyResponse_cachesLibraryRules()
            throws OpenAgencyConnectorException {
        when(connector.getLibraryRules(123456, null))
                .thenReturn(Optional.of(new dk.dbc.oss.ns.openagency.LibraryRules()));
        final AgencyConnection agencyConnection = newAgencyConnection();
        assertSame(agencyConnection.getLibraryRules(123456, null),
                agencyConnection.getLibraryRules(123456, null));

        verify(connector, times(1)).getLibraryRules(123456, null);
    }

    @Test
    public void getLibraryRules_connectorReturnsEmptyResponse_notCached()
            throws OpenAgencyConnectorException {
        when(connector.getLibraryRules(123456, null))
                .thenReturn(Optional.empty());
        final AgencyConnection agencyConnection = newAgencyConnection();
        agencyConnection.getLibraryRules(123456, null);
        agencyConnection.getLibraryRules(123456, null);

        verify(connector, times(2)).getLibraryRules(123456, null);
    }

    @Test
    public void getFbsImsLibraries_connectorThrows_throws()
            throws OpenAgencyConnectorException {
        when(connector.getFbsImsLibraries())
                .thenThrow(new OpenAgencyConnectorException("died"));
        assertThat(() -> newAgencyConnection().getFbsImsLibraries(),
                isThrowing(HarvesterException.class));
    }

    private AgencyConnection newAgencyConnection() {
        return new AgencyConnection(connector);
    }
}