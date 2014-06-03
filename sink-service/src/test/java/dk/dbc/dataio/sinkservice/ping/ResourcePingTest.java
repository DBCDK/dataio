package dk.dbc.dataio.sinkservice.ping;

import dk.dbc.dataio.commons.types.PingResponse;
import org.junit.Before;
import org.junit.Test;

import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.sql.DataSource;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.any;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

/**
  * DataSourcePing unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class ResourcePingTest {
    private InitialContext context;
    private static final String JDBC_RESOURCE_NAME = "jdbc/db";
    private static final String URL_RESOURCE_NAME = "url/path";

    @Before
    public void setup() throws Exception {
        context = mock(InitialContext.class);
    }

    @Test(expected = NullPointerException.class)
    public void execute_contextArgIsNull_throws() throws Exception {
        ResourcePing.execute(null, URL_RESOURCE_NAME, String.class);
    }

    @Test(expected = NullPointerException.class)
    public void execute_resourceNameArgIsNull_throws() throws Exception {
        ResourcePing.execute(context, null, String.class);
    }

    @Test(expected = IllegalArgumentException.class)
    public void execute_resourceNameArgIsEmpty_throws() throws Exception {
        ResourcePing.execute(context, "", String.class);
    }

    @Test(expected = NullPointerException.class)
    public void execute_resourceClassArgIsNull_throws() throws Exception {
        ResourcePing.execute(context, URL_RESOURCE_NAME, null);
    }

    @Test
    public void execute_requiredResourceIsNotAvailable_returnsPingResponseWithStatusFailed() throws Exception {
        when(context.lookup(any(String.class))).thenThrow(new NamingException());

        final PingResponse response = ResourcePing.execute(context, URL_RESOURCE_NAME, String.class);
        assertThat(response.getStatus(), is(PingResponse.Status.FAILED));
        assertThat(response.getLog().size(), is(1));
    }

    @Test
    public void execute_requiredUrlResourceIsAvailable_returnsPingResponseWithStatusOk() throws Exception {
        when(context.lookup(any(String.class))).thenReturn("resource");

        final PingResponse response = ResourcePing.execute(context, URL_RESOURCE_NAME, String.class);
        assertThat(response.getStatus(), is(PingResponse.Status.OK));
        assertThat(response.getLog().size(), is(1));
    }

    @Test
    public void execute_requiredJdbcResourceIsAvailable_returnsPingResponseWithStatusOk() throws Exception {
        final DataSource dataSource = mock(DataSource.class);
        when(context.lookup(any(String.class))).thenReturn(dataSource);

        final PingResponse response = ResourcePing.execute(context, JDBC_RESOURCE_NAME, DataSource.class);
        assertThat(response.getStatus(), is(PingResponse.Status.OK));
        assertThat(response.getLog().size(), is(1));
    }

    @Test
    public void execute_requiredResourceIsAvailableButOfWrongType_returnsPingResponseWithStatusFailed() throws Exception {
        when(context.lookup(any(String.class))).thenReturn(new Object());

        final PingResponse response = ResourcePing.execute(context, URL_RESOURCE_NAME, String.class);
        assertThat(response.getStatus(), is(PingResponse.Status.FAILED));
        assertThat(response.getLog().size(), is(1));
    }
}
