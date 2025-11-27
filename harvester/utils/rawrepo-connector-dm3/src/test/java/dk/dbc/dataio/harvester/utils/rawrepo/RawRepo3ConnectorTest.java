package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.sql.DataSource;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;

public class RawRepo3ConnectorTest {
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
        assertThrows(NullPointerException.class, () -> new RawRepo3Connector((String) null, null));
    }

    @Test
    public void constructor_dataSourceResourceNameIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RawRepo3Connector("", null));
    }

    @Test
    public void constructor_dataSourceResourceNameLookupThrowsNamingException_throws() {
        assertThrows(IllegalStateException.class, () -> new RawRepo3Connector("noSuchResource", null));
    }

    @Test
    public void constructor_dataSourceResourceNameLookupReturnsNonDataSourceObject_throws() {
        InMemoryInitialContextFactory.bind(DATA_SOURCE_RESOURCE_NAME, "notDataSource");
        assertThrows(IllegalStateException.class, () -> new RawRepo3Connector(DATA_SOURCE_RESOURCE_NAME, "test"));
    }

    @Test
    public void constructor_resolvesDataSourceName() {
        RawRepo3Connector rawRepoConnector = new RawRepo3Connector(DATA_SOURCE_RESOURCE_NAME, "test");
        assertThat("connector.dataSource", rawRepoConnector.getDataSource(), is(dataSource));
    }

    @Test
    public void dequeue_consumerIdArgIsNull_throws() {
        assertThrows(NullPointerException.class, () ->  new RawRepo3Connector(DATA_SOURCE_RESOURCE_NAME, null));
    }

    @Test
    public void dequeue_consumerIdArgIsEmpty_throws() {
        assertThrows(IllegalArgumentException.class, () -> new RawRepo3Connector(DATA_SOURCE_RESOURCE_NAME, ""));
    }

    @Test
    public void dequeue_consumerIsNull_throws() throws SQLException {
        RawRepo3Connector connector = new RawRepo3Connector(DATA_SOURCE_RESOURCE_NAME, "test");
        assertThrows(NullPointerException.class, () -> connector.dequeue(1, null));
    }
}
