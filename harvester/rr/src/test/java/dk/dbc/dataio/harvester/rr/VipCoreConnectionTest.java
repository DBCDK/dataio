package dk.dbc.dataio.harvester.rr;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.vipcore.exception.AgencyNotFoundException;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import dk.dbc.vipcore.marshallers.LibraryRule;
import dk.dbc.vipcore.marshallers.LibraryRules;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.any;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyObject;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class VipCoreConnectionTest {
    private final VipCoreLibraryRulesConnector connector = mock(VipCoreLibraryRulesConnector.class);

    @Test
    public void getLibraryRules_connectorReturnsEmptyResponse_returnsNull()
            throws VipCoreException {
        when(connector.getLibraryRulesByAgencyId("123456", null))
                .thenThrow(new AgencyNotFoundException());
        assertNull(newVipCoreConnection().getLibraryRules(123456, null));
    }

    @Test
    public void getLibraryRules_connectorReturnsNonEmptyResponse_returnsLibraryRules() throws VipCoreException {
        final LibraryRule firstLibraryRule = new LibraryRule();
        firstLibraryRule.setName("1st rule");
        firstLibraryRule.setBool(true);
        final LibraryRule secondLibraryRule = new LibraryRule();
        secondLibraryRule.setName("2nd rule");
        secondLibraryRule.setBool(false);
        final LibraryRule thirdLibraryRule = new LibraryRule();
        thirdLibraryRule.setName("3rd rule");
        thirdLibraryRule.setString("value");

        final LibraryRules libraryRules = new LibraryRules();
        libraryRules.setAgencyType("myType");
        libraryRules.setLibraryRule(Arrays.asList(firstLibraryRule, secondLibraryRule, thirdLibraryRule));

        final AddiMetaData.LibraryRules expectedLibraryRules = new AddiMetaData.LibraryRules()
                .withAgencyType(libraryRules.getAgencyType())
                .withLibraryRule(firstLibraryRule.getName(), firstLibraryRule.getBool())
                .withLibraryRule(secondLibraryRule.getName(), secondLibraryRule.getBool())
                .withLibraryRule(thirdLibraryRule.getName(), thirdLibraryRule.getString());

        when(connector.getLibraryRulesByAgencyId(anyString(), anyString()))
                .thenReturn(libraryRules);
        assertThat(newVipCoreConnection().getLibraryRules(123456, "test"),
                is(expectedLibraryRules));
    }


    @Test
    public void getLibraryRules_connectorThrows_throws()
            throws VipCoreException {
        when(connector.getLibraryRulesByAgencyId(anyString(), anyString()))
                .thenThrow(new VipCoreException("died"));
        assertThat(() -> newVipCoreConnection().getLibraryRules(123456, "test"),
                isThrowing(IllegalStateException.class));
    }

    @Test
    public void getLibraryRules_connectorReturnsNonEmptyResponse_cachesLibraryRules()
            throws VipCoreException {
        final LibraryRules emptyLibraryRules = new LibraryRules();
        emptyLibraryRules.setLibraryRule(new ArrayList<>());

        when(connector.getLibraryRulesByAgencyId("123456", null))
                .thenReturn(emptyLibraryRules);
        final VipCoreConnection vipCoreConnection = newVipCoreConnection();
        assertSame(vipCoreConnection.getLibraryRules(123456, null),
                vipCoreConnection.getLibraryRules(123456, null));

        verify(connector, times(1)).getLibraryRulesByAgencyId("123456", null);
    }

    @Test
    public void getLibraryRules_connectorReturnsEmptyResponse_notCached()
            throws VipCoreException {
        when(connector.getLibraryRulesByAgencyId("123456", null))
                .thenThrow(new AgencyNotFoundException());
        final VipCoreConnection agencyConnection = newVipCoreConnection();
        agencyConnection.getLibraryRules(123456, null);
        agencyConnection.getLibraryRules(123456, null);

        verify(connector, times(2)).getLibraryRulesByAgencyId("123456", null);
    }

    @Test
    public void getFbsImsLibraries_connectorThrows_throws()
            throws VipCoreException {
        when(connector.getLibrariesByLibraryRule(anyObject(), anyBoolean()))
                .thenThrow(new VipCoreException("died"));
        assertThat(() -> newVipCoreConnection().getFbsImsLibraries(),
                isThrowing(HarvesterException.class));
    }

    private VipCoreConnection newVipCoreConnection() {
        return new VipCoreConnection(connector);
    }
}
