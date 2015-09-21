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

package dk.dbc.dataio.logstore.service.connector.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.naming.Context;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class LogStoreServiceConnectorBeanTest {
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
        final LogStoreServiceConnectorBean logStoreServiceConnectorBean = newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void getConnector_connectorIsSet_connectorIsReturned() {
        LogStoreServiceConnector logStoreServiceConnector = mock(LogStoreServiceConnector.class);
        LogStoreServiceConnectorBean logStoreServiceConnectorBean = newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.logStoreServiceConnector = logStoreServiceConnector;
        assertThat(logStoreServiceConnectorBean.getConnector(), is(logStoreServiceConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() throws Exception{
        InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_LOGSTORE_RS, "someURL");
        LogStoreServiceConnectorBean logStoreServiceConnectorBean = newLogStoreServiceConnectorBean();
        logStoreServiceConnectorBean.initializeConnector();
        assertThat(logStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

   /*
    * Private methods
    */
    private LogStoreServiceConnectorBean newLogStoreServiceConnectorBean() {
        return new LogStoreServiceConnectorBean();
    }
}