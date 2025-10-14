package dk.dbc.dataio.harvester.rr_dm3;

import dk.dbc.dataio.commons.types.AddiMetaData;
import dk.dbc.dataio.harvester.types.HarvesterException;
import dk.dbc.vipcore.exception.AgencyNotFoundException;
import dk.dbc.vipcore.exception.VipCoreException;
import dk.dbc.vipcore.libraryrules.VipCoreLibraryRulesConnector;
import dk.dbc.vipcore.marshallers.LibraryRule;
import dk.dbc.vipcore.marshallers.LibraryRules;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
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
        Assertions.assertNull(newVipCoreConnection().getLibraryRules(123456, null));
    }

    @Test
    public void getLibraryRules_connectorReturnsNonEmptyResponse_returnsLibraryRules() throws VipCoreException {
        LibraryRule firstLibraryRule = new LibraryRule();
        firstLibraryRule.setName("1st rule");
        firstLibraryRule.setBool(true);
        LibraryRule secondLibraryRule = new LibraryRule();
        secondLibraryRule.setName("2nd rule");
        secondLibraryRule.setBool(false);
        LibraryRule thirdLibraryRule = new LibraryRule();
        thirdLibraryRule.setName("3rd rule");
        thirdLibraryRule.setString("value");

        LibraryRules libraryRules = new LibraryRules();
        libraryRules.setAgencyType("myType");
        libraryRules.setLibraryRule(Arrays.asList(firstLibraryRule, secondLibraryRule, thirdLibraryRule));

        AddiMetaData.LibraryRules expectedLibraryRules = new AddiMetaData.LibraryRules()
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
        LibraryRules emptyLibraryRules = new LibraryRules();
        emptyLibraryRules.setLibraryRule(new ArrayList<>());

        when(connector.getLibraryRulesByAgencyId("123456", null))
                .thenReturn(emptyLibraryRules);
        VipCoreConnection vipCoreConnection = newVipCoreConnection();
        Assertions.assertSame(vipCoreConnection.getLibraryRules(123456, null), vipCoreConnection.getLibraryRules(123456, null));

        verify(connector, times(1)).getLibraryRulesByAgencyId("123456", null);
    }

    @Test
    public void getLibraryRules_connectorReturnsEmptyResponse_notCached()
            throws VipCoreException {
        when(connector.getLibraryRulesByAgencyId("123456", null))
                .thenThrow(new AgencyNotFoundException());
        VipCoreConnection agencyConnection = newVipCoreConnection();
        agencyConnection.getLibraryRules(123456, null);
        agencyConnection.getLibraryRules(123456, null);

        verify(connector, times(2)).getLibraryRulesByAgencyId("123456", null);
    }

    @Test
    public void getFbsImsLibraries_connectorThrows_throws()
            throws VipCoreException {
        when(connector.getLibraries(any()))
                .thenThrow(new VipCoreException("died"));
        assertThat(() -> newVipCoreConnection().getFbsImsLibraries(),
                isThrowing(HarvesterException.class));
    }

    private VipCoreConnection newVipCoreConnection() {
        return new VipCoreConnection(connector);
    }
}
