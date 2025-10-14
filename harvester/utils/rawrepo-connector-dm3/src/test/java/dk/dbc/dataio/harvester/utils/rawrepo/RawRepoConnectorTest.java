package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.sql.DataSource;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class RawRepoConnectorTest {
    private static final String DATA_SOURCE_RESOURCE_NAME = "resourceName";

    private final DataSource dataSource = mock(DataSource.class);

    @BeforeAll
    public static void setupClass() {
        // sets up the InMemoryInitialContextFactory as default factory.
        System.setProperty(Context.INITIAL_CONTEXT_FACTORY, InMemoryInitialContextFactory.class.getName());
    }

    @BeforeEach
    public void setupTest() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, dataSource);
    }

    @Test
    public void constructor_dataSourceResourceNameIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new RawRepoConnector((String) null));
    }

    @Test
    public void constructor_dataSourceResourceNameIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RawRepoConnector(""));
    }

    @Test
    public void constructor_dataSourceResourceNameLookupThrowsNamingException_throws() {
        assertThrows(IllegalStateException.class, () -> new RawRepoConnector("noSuchResource"));
    }

    @Test
    public void constructor_dataSourceResourceNameLookupReturnsNonDataSourceObject_throws() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, "notDataSource");
        assertThrows(IllegalStateException.class, () -> new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME));
    }

    @Test
    public void constructor_resolvesDataSourceName() {
        RawRepoConnector rawRepoConnector = new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME);
        assertThat("connector.dataSource", rawRepoConnector.getDataSource(), is(dataSource));
    }

    @Test
    public void dequeue_consumerIdArgIsNull_throws() {
        RawRepoConnector rawRepoConnector = new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME);
        assertThrows(NullPointerException.class, () -> rawRepoConnector.dequeue(null));
    }

    @Test
    public void dequeue_consumerIdArgIsEmpty_throws() {
        RawRepoConnector rawRepoConnector = new RawRepoConnector(DATA_SOURCE_RESOURCE_NAME);
        assertThrows(IllegalArgumentException.class, () -> rawRepoConnector.dequeue(""));
    }
}
