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

package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.naming.Context;
import javax.sql.DataSource;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;

public class RawRepoConnectorTest {
    private static final String DATA_SOURCE_RESOURCE_NAME = "resourceName";

    private final DataSource dataSource = mock(DataSource.class);
    private final RawRepoDAO rawRepoDAO = mock(RawRepoDAO.class);
    private final RelationHintsOpenAgency relationHints = mock(RelationHintsOpenAgency.class);

    @BeforeClass
    public static void setupClass() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @Before
    public void setupTest() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, dataSource);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dataSourceResourceNameIsNull_throws() {
        new RawRepoConnector((String) null, relationHints);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_dataSourceIsNull_throws() {
        new RawRepoConnector((DataSource) null, relationHints);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_dataSourceResourceNameIsEmpty_throws() {
        new RawRepoConnector("", relationHints);
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_dataSourceResourceNameLookupThrowsNamingException_throws() {
        new RawRepoConnector("noSuchResource", relationHints);
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_dataSourceResourceNameLookupReturnsNonDataSourceObject_throws() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, "notDataSource");
        new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME, relationHints);
    }

    @Test(expected = NullPointerException.class)
    public void constructor_relationalHintsIsNull_throws() {
        new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME, null);
    }

    @Test
    public void constructor_dataSourceResourceNameLookupReturnsDataSourceObject_returnsNewInstance() {
        final RawRepoConnector connector = getRawRepoConnector();
        assertThat("connector", connector, is(notNullValue()));
        assertThat("connector.dataSource", connector.getDataSource(), is(dataSource));
    }

    @Test
    public void dequeue_consumerIdArgIsNull_throws() throws SQLException, RawRepoException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.dequeue(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void dequeue_consumerIdArgIsEmpty_throws() throws SQLException, RawRepoException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.dequeue("");
            fail("No exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void queueFail_queueJobsArgIsNull_throws() throws SQLException, RawRepoException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.queueFail(null, "error");
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    private RawRepoConnector getRawRepoConnector() {
        return new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME, relationHints);
    }
}
