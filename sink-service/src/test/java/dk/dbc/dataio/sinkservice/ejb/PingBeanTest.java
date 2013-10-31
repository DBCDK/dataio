package dk.dbc.dataio.sinkservice.ejb;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.ejb.EJBException;
import javax.naming.InitialContext;
import javax.naming.NamingException;
import javax.ws.rs.core.Response;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.powermock.api.mockito.PowerMockito.whenNew;

/**
  * PingBean unit tests
  * <p>
  * The test methods of this class uses the following naming convention:
  *
  *  unitOfWork_stateUnderTest_expectedBehavior
  */
@RunWith(PowerMockRunner.class)
@PrepareForTest({
    PingBean.class,
})
public class PingBeanTest {
    @Test(expected = NullPointerException.class)
    public void ping_sinkContentDataIsNull_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void ping_sinkContentDataIsEmpty_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping("");
    }

    @Test(expected = JsonException.class)
    public void ping_sinkContentDataIsInvalidJson_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping("{");
    }

    @Test(expected = JsonException.class)
    public void ping_sinkContentDataIsInvalidSinkContent_throws() throws Exception {
        final PingBean pingBean = new PingBean();
        pingBean.ping("{\"name\": \"name\"}");
    }

    @Test(expected = EJBException.class)
    public void ping_initialContextCreationThrows_throws() throws Exception {
        whenNew(InitialContext.class).withNoArguments().thenThrow(new NamingException());
        final PingBean pingBean = new PingBean();
        pingBean.ping(getValidSinkContent());
    }

    @Test
    public void ping_pingIsExecuted_returnsOkResponse() throws Exception {
        final PingBean pingBean = new PingBean();
        final Response response = pingBean.ping(getValidSinkContent());
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
    }

    private String getValidSinkContent() throws JsonException {
        final SinkContent sinkContent = new SinkContent("name", "dataio/resource");
        return JsonUtil.toJson(sinkContent);
    }
}
