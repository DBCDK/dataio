package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import dk.dbc.marcxmerge.MarcXMergerException;
import dk.dbc.rawrepo.RawRepoException;
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
        new RawRepoConnector(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor_dataSourceResourceNameIsEmpty_throws() {
        new RawRepoConnector("");
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_dataSourceResourceNameLookupThrowsNamingException_throws() {
        new RawRepoConnector("noSuchResource");
    }

    @Test(expected = IllegalStateException.class)
    public void constructor_dataSourceResourceNameLookupReturnsNonDataSourceObject_throws() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, "notDataSource");
        new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME);
    }

    @Test
    public void constructor_dataSourceResourceNameLookupReturnsDataSourceObject_returnsNewInstance() {
        final RawRepoConnector connector = getRawRepoConnector();
        assertThat("connector", connector, is(notNullValue()));
        assertThat("connector.dataSource", connector.getDataSource(), is(dataSource));
    }

    @Test
    public void fetchRecord_idArgIsNull_throws() throws SQLException, RawRepoException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.fetchRecord(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void fetchRecordCollection_idArgIsNull_throws() throws SQLException, RawRepoException, MarcXMergerException {
        final RawRepoConnector connector = getRawRepoConnector();
        try {
            connector.fetchRecordCollection(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
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
        return new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME);
    }
}