package dk.dbc.dataio.harvester.utils.rawrepo;

import dk.dbc.dataio.commons.utils.test.jndi.InMemoryInitialContextFactory;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import javax.naming.Context;
import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

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

    @Test
    void getUrl_resolvesValuePerDataSource() throws SQLException {
        DataSource test = mockConfigurationsTable("kat-search-zk.datawell-test.svc.cloud.dbc.dk/prokat");
        DataSource staging = mockConfigurationsTable("kat-search-zk.datawell-staging.svc.cloud.dbc.dk/prokat");

        assertThat("test", RawRepo3Connector.RawRepoService.SOLR_ZK.getUrl(test),
                is("kat-search-zk.datawell-test.svc.cloud.dbc.dk/prokat"));
        assertThat("staging", RawRepo3Connector.RawRepoService.SOLR_ZK.getUrl(staging),
                is("kat-search-zk.datawell-staging.svc.cloud.dbc.dk/prokat"));
    }

    @Test
    void getUrl_cachesValuePerDataSourceAndService() throws SQLException {
        DataSource ds = mockConfigurationsTable("http://record-service");

        RawRepo3Connector.RawRepoService.RECORD_SERVICE.getUrl(ds);
        RawRepo3Connector.RawRepoService.RECORD_SERVICE.getUrl(ds);

        verify(ds, times(1)).getConnection();
    }

    @Test
    void getUrl_closesConnection() throws SQLException {
        DataSource ds = mockConfigurationsTable("http://vipcore");
        Connection connection = ds.getConnection();

        RawRepo3Connector.RawRepoService.VIPCORE.getUrl(ds);

        verify(connection).close();
    }

    @Test
    void getUrl_valueMissing_throwsAndClosesConnection() throws SQLException {
        DataSource ds = mockConfigurationsTable(null);
        Connection connection = ds.getConnection();

        assertThrows(IllegalStateException.class, () -> RawRepo3Connector.RawRepoService.OPENAGENCY.getUrl(ds));
        verify(connection).close();
    }

    private static DataSource mockConfigurationsTable(String value) throws SQLException {
        DataSource ds = mock(DataSource.class);
        Connection connection = mock(Connection.class);
        PreparedStatement ps = mock(PreparedStatement.class);
        ResultSet rs = mock(ResultSet.class);
        when(ds.getConnection()).thenReturn(connection);
        when(connection.prepareStatement("select value from configurations where key = ?")).thenReturn(ps);
        when(ps.executeQuery()).thenReturn(rs);
        when(rs.next()).thenReturn(value != null);
        when(rs.getString(1)).thenReturn(value);
        return ds;
    }
}
