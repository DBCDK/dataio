package dk.dbc.dataio.sinkservice.ping;

import dk.dbc.dataio.commons.types.PingResponse;
import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.test.model.SinkContentBuilder;
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
  * EsPing unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
public class EsPingTest {
    private InitialContext context;

    @Before
    public void setup() throws Exception {
        context = mock(InitialContext.class);
    }

    @Test(expected = NullPointerException.class)
    public void execute_contextArgIsNull_throws() throws Exception {
        EsPing.execute(null, getSinkContent());
    }

    @Test(expected = NullPointerException.class)
    public void execute_sinkContentArgIsNull_throws() throws Exception {
        EsPing.execute(context, null);
    }

    @Test
    public void execute_requiredResourceIsNotAvailable_returnsPingResponseWithStatusFailed() throws Exception {
        when(context.lookup(any(String.class))).thenThrow(new NamingException());

        final PingResponse response = EsPing.execute(context, getSinkContent());
        assertThat(response.getStatus(), is(PingResponse.Status.FAILED));
        assertThat(response.getLog().size(), is(1));
    }

    @Test
    public void execute_requiredResourceIsAvailable_returnsPingResponseWithStatusOk() throws Exception {
        final DataSource dataSource = mock(DataSource.class);
        when(context.lookup(any(String.class))).thenReturn(dataSource);

        final PingResponse response = EsPing.execute(context, getSinkContent());
        assertThat(response.getStatus(), is(PingResponse.Status.OK));
        assertThat(response.getLog().size(), is(1));
    }

    @Test
    public void execute_requiredResourceIsAvailableButOfWrongType_returnsPingResponseWithStatusFailed() throws Exception {
        when(context.lookup(any(String.class))).thenReturn(new Object());

        final PingResponse response = EsPing.execute(context, getSinkContent());
        assertThat(response.getStatus(), is(PingResponse.Status.FAILED));
        assertThat(response.getLog().size(), is(1));
    }

    private SinkContent getSinkContent() {
        return new SinkContentBuilder().build();
    }
}
