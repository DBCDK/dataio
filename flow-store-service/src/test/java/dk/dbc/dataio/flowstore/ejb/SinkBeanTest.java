package dk.dbc.dataio.flowstore.ejb;

import dk.dbc.dataio.commons.utils.json.JsonException;
import javax.persistence.EntityManager;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;
import org.junit.Test;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

public class SinkBeanTest {

    @Test
    public void sinkBean_validConstructor_newInstance() {
        SinksBean sink = new SinksBean();
    }

    @Test(expected = NullPointerException.class)
    public void createSink_nullSinkContent_throws() throws JsonException {
        SinksBean sink = new SinksBean();
        sink.createSink(null, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void createSink_emptySinkContent_throws() throws JsonException {
        SinksBean sink = new SinksBean();
        sink.createSink(null, "");
    }

    @Test(expected = JsonException.class)
    public void createSink_invalidJSON_throwsJsonException() throws JsonException {
        SinksBean sink = new SinksBean();
        EntityManager entityManager = mock(EntityManager.class);
        sink.entityManager = entityManager;

        UriInfo uriInfo = mock(UriInfo.class);
        UriBuilder uriBuilder = UriBuilder.fromPath("http://localhost:8080/sink");
        when(uriInfo.getAbsolutePathBuilder()).thenReturn(uriBuilder);

        Response response = sink.createSink(uriInfo, "invalid Json");
    }
}