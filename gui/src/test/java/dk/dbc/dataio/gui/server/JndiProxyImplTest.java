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

package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;

import static org.hamcrest.core.Is.is;
import static org.junit.Assert.assertThat;

public class JndiProxyImplTest {
    protected static final String JNDI_NAME = "jndi-name";
    protected static final String JNDI_VALUE = "jndi-value";


    @BeforeClass
    public static void setInitialContextFactory() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setInitialContext() {
        InMemoryInitialContextFactory.bind(JNDI_NAME, JNDI_VALUE);
    }

    @After
    public void clearInitialContext() {
        InMemoryInitialContextFactory.clear();
    }


    /*
     * Test getJndiResource
     */

    @Test(expected = NullPointerException.class)
    public void getJndiResource_nullInput_throws() {
        // Test preparation
        JndiProxyImpl jndi = new JndiProxyImpl();

        // Test subject under test
        jndi.getJndiResource(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void getJndiResource_emptyInput_throws() {
        // Test preparation
        JndiProxyImpl jndi = new JndiProxyImpl();

        // Test subject under test
        jndi.getJndiResource("");
    }

    @Test
    public void getJndiResource_validInputHitsSystemProperty_validStringOutput() {
        // Test preparation
        JndiProxyImpl jndi = new JndiProxyImpl();
        System.setProperty("resourceName", "resource-value");

        // Test subject under test
        String resource = jndi.getJndiResource("resourceName");

        // Test validation
        assertThat(resource, is("resource-value"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getJndiResource_validInputDoesNotHitSystemProperty_illegalArgumentException() {
        // Test preparation
        JndiProxyImpl jndi = new JndiProxyImpl();
        System.setProperty("resourceName", "resource-value");

        // Test subject under test
        String resource = jndi.getJndiResource("resourceNameNoHit");
    }

    @Test
    public void getJndiResource_validInputHitsJndiProperty_validStringOutput() {
        // Test preparation
        JndiProxyImpl jndi = new JndiProxyImpl();

        // Test subject under test
        String jndiValue = jndi.getJndiResource(JNDI_NAME);

        // Test validation
        assertThat(jndiValue, is(JNDI_VALUE));
    }

    @Test(expected = IllegalArgumentException.class)
    public void getJndiResource_validInputDoesNotHitJndiProperty_illegalArgumentException() {
        // Test preparation
        JndiProxyImpl jndi = new JndiProxyImpl();

        // Test subject under test
        String jndiValue = jndi.getJndiResource("NonExsistingJndiName");
    }

}
