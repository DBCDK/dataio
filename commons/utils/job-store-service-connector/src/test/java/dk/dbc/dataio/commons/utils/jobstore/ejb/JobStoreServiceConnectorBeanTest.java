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

package dk.dbc.dataio.commons.utils.jobstore.ejb;

import dk.dbc.dataio.commons.types.jndi.JndiConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.naming.Context;

import static org.hamcrest.CoreMatchers.not;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.core.Is.is;
import static org.mockito.Mockito.mock;

public class JobStoreServiceConnectorBeanTest {
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
        final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = newJobStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
    }

    @Test
    public void getConnector_connectorIsSet_connectorIsReturned() {
        JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
        JobStoreServiceConnectorBean jobStoreServiceConnectorBean = newJobStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.jobStoreServiceConnector = jobStoreServiceConnector;
        assertThat(jobStoreServiceConnectorBean.getConnector(), is(jobStoreServiceConnector));
    }

    @Test
    public void initializeConnector_connectorIsInitialized_connectorIsNotNull() throws Exception{
        InMemoryInitialContextFactory.bind(JndiConstants.URL_RESOURCE_JOBSTORE_RS, "someURL");
        JobStoreServiceConnectorBean jobStoreServiceConnectorBean = newJobStoreServiceConnectorBean();
        jobStoreServiceConnectorBean.initializeConnector();
        assertThat(jobStoreServiceConnectorBean.getConnector(), not(nullValue()));
    }

   /*
    * Private methods
    */
    private JobStoreServiceConnectorBean newJobStoreServiceConnectorBean() {
        return new JobStoreServiceConnectorBean();
    }
}
