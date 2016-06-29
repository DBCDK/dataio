/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This
 * ile is part of DataIO.
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

package dk.dbc.dataio.commons.utils.ush.solr.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.commons.utils.ush.solr.UshSolrHarvesterConnector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.naming.Context;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsNull.nullValue;
import static org.mockito.Mockito.mock;

public class UshSolrHarvesterConnectorBeanTest {

    @BeforeClass
    public static void setup() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void clearContext() {
        InMemoryInitialContextFactory.clear();
    }

    @Test(expected = EJBException.class)
    public void initializeConnector_urlResourceLookupThrowsNamingException_throws() {
        final UshSolrHarvesterConnectorBean ushSolrHarvesterConnectorBean = newUshSolrHarvesterConnectorBean();
        ushSolrHarvesterConnectorBean.initializeConnector();
    }

    @Test
    public void getConnector_connectorIsInitialized_connectorIsReturned() {
        UshSolrHarvesterConnector ushSolrHarvesterConnector = mock(UshSolrHarvesterConnector.class);
        UshSolrHarvesterConnectorBean ushSolrHarvesterConnectorBean = newUshSolrHarvesterConnectorBean();
        ushSolrHarvesterConnectorBean.ushSolrHarvesterConnector = ushSolrHarvesterConnector;
        assertThat(ushSolrHarvesterConnectorBean.getConnector(), is(ushSolrHarvesterConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() {
        InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_USH_HARVESTER, "someURL");
        UshSolrHarvesterConnectorBean ushSolrHarvesterConnectorBean = newUshSolrHarvesterConnectorBean();
        ushSolrHarvesterConnectorBean.initializeConnector();
        assertThat(ushSolrHarvesterConnectorBean.getConnector(), not(nullValue()));
    }

    /*
     * Private methods
     */
    private UshSolrHarvesterConnectorBean newUshSolrHarvesterConnectorBean() {
        return new UshSolrHarvesterConnectorBean();
    }
}
