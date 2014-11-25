package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.ejb.JSONBBean;
import org.junit.Before;
import org.junit.Test;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class PgJobStoreTest {
    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    final DataSource dataSource = mock(DataSource.class);
    final Connection connection = mock(Connection.class);
    final PreparedStatement preparedStatement = mock(PreparedStatement.class);
    final ResultSet resultSet = mock(ResultSet.class);

    @Before
    public void setUpMocks() throws SQLException {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);
    }

    @Test
    public void addEntity_entityArgIsNull_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addEntity(null);
            fail("No exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void addEntity_entityArgCanNotBeMarshalled_throws() throws JobStoreException {
        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addEntity(new Object());
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addEntity_onDatabaseAccessError_throws() throws JobStoreException, SQLException {
        when(dataSource.getConnection()).thenThrow(new SQLException());

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addEntity(new Object());
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addEntity_databaseReturnsMultipleRows_throws() throws JobStoreException, SQLException {
        when(resultSet.next()).thenReturn(true);
        when(resultSet.isLast()).thenReturn(false);

        final PgJobStore pgJobStore = newPgJobStore();
        try {
            pgJobStore.addEntity(new Object());
            fail("No exception thrown");
        } catch (JobStoreException e) {
        }
    }

    @Test
    public void addEntity_databaseReturnsSingleRow_returnsValueOfRowColumn() throws JobStoreException, SQLException {
        final int id = 42;
        when(resultSet.next()).thenReturn(true);
        when(resultSet.isLast()).thenReturn(true);
        when(resultSet.getInt(1)).thenReturn(id);

        final SimpleBean simpleBean = new SimpleBean();
        simpleBean.setValue("myValue");

        final PgJobStore pgJobStore = newPgJobStore();
        assertThat(pgJobStore.addEntity(simpleBean), is(id));
    }

    private PgJobStore newPgJobStore() {
        final JSONBBean jsonbBean = new JSONBBean();
        jsonbBean.initialiseContext();

        final PgJobStore pgJobStore = new PgJobStore();
        pgJobStore.jsonbBean = jsonbBean;
        pgJobStore.dataSource = dataSource;

        return pgJobStore;
    }

    private static class SimpleBean {
        String value;
        public String getValue() {
            return value;
        }
        public void setValue(String value) {
            this.value = value;
        }
    }
}