package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.JsonNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.exceptions.ReferencedEntityNotFoundException;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderContentJsonBuilder;
import dk.dbc.dataio.commons.utils.test.json.FlowBinderJsonBuilder;
import dk.dbc.dataio.flowstore.entity.FlowBinder;
import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class FlowBindersBeanTest {
    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);

    private final JSONBContext jsonbContext = new JSONBContext();

    @Test
    @SuppressWarnings("unchecked")
    public void getAllFlowBinders_validParametersAndMatchingFlow_returnsResponseWithFlowAndHTTP200() throws JSONBException {
        FlowBindersBean fbb = new FlowBindersBean();
        EntityManager entityManager = mock(EntityManager.class);
        fbb.entityManager = entityManager;
        TypedQuery<dk.dbc.dataio.commons.types.FlowBinder> query = mock(TypedQuery.class);
        when(entityManager.createNamedQuery(FlowBinder.FIND_ALL_QUERY_NAME, dk.dbc.dataio.commons.types.FlowBinder.class)).thenReturn(query);

        String flowBinderStr = new FlowBinderJsonBuilder().build();
        dk.dbc.dataio.commons.types.FlowBinder flowBinder = jsonbContext.unmarshall(flowBinderStr, dk.dbc.dataio.commons.types.FlowBinder.class);

        when(query.getResultList()).thenReturn(Collections.singletonList(flowBinder));
        Response response = fbb.findAllFlowBinders();
        assertThat(response.getStatus(), is(200));
    }

    @Test
    public void getFlowBinderById_flowBinderNotFound_returnsResponse404() throws JSONBException {
        final long FLOW_BINDER_ID = 12L;
        FlowBindersBean bean = new FlowBindersBean();
        EntityManager mockedEntityManager = mock(EntityManager.class);
        bean.entityManager = mockedEntityManager;
        when(mockedEntityManager.find(FlowBinder.class, FLOW_BINDER_ID)).thenReturn(null);  // null => Not found

        Response response = bean.getFlowBinderById(FLOW_BINDER_ID);

        assertThat(response.getStatus(), is(404));
    }

    @Test
    public void getFlowBinderById_flowBinderFound_returnsResponseWithFlowAndHTTP200() throws JSONBException {
        final long FLOW_BINDER_ID = 12L;
        FlowBindersBean bean = new FlowBindersBean();
        EntityManager mockedEntityManager = mock(EntityManager.class);
        bean.entityManager = mockedEntityManager;
        FlowBinder flowBinder = testFlowBinder();
        when(mockedEntityManager.find(FlowBinder.class, FLOW_BINDER_ID)).thenReturn(flowBinder);

        Response response = bean.getFlowBinderById(FLOW_BINDER_ID);

        assertThat(response.getStatus(), is(200));
        assertThat(response.hasEntity(), is(true));
        JsonNode entity = jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entity.get("version").asLong(), is(FLOW_BINDER_VERSION));
        assertThat(entity.get("content").get("flowId").asLong(), is(flowBinder.getFlowId()));
        assertThat(entity.get("content").get("sinkId").asLong(), is((flowBinder.getSinkId())));
    }

    @Test
    public void updateFlowBinder_nullFlowBinderContent_throws() {
        assertThrows(NullPointerException.class, () -> newFlowBindersBeanWithMockedEntityManager().updateFlowBinder(null, 1L, 1L));
    }

    @Test
    public void updateFlowBinder_emptyFlowBinderContent_throws() throws JSONBException, ReferencedEntityNotFoundException {
        assertThrows(IllegalArgumentException.class, () -> newFlowBindersBeanWithMockedEntityManager().updateFlowBinder("", 1L, 1L));
    }

    @Test
    public void deleteFlowBinder_flowBinderNotFound_returnsResponseWithHttpStatusNotFound() {
        FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(null);

        Response response = flowBindersBean.deleteFlowBinder(12L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteFlowBinder_flowBinderFound_returnsNoContentHttpResponse() {
        FlowBinder flowBinder = mock(FlowBinder.class);
        FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(FlowBinder.class), any())).thenReturn(flowBinder);
        when(ENTITY_MANAGER.merge(any(FlowBinder.class))).thenReturn(flowBinder);

        Response response = flowBindersBean.deleteFlowBinder(12L, 1L);

        verify(flowBinder).setVersion(1L);
        verify(ENTITY_MANAGER).remove(flowBinder);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    @Test
    public void deleteFlowBinder_errorWhileSettingParametersForQuery_returnsResponseWithHttpStatusNotFound() {
        FlowBinder flowBinder = mock(FlowBinder.class);
        FlowBindersBean flowBindersBean = newFlowBindersBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(FlowBinder.class, 0)).thenReturn(flowBinder);

        Response response = flowBindersBean.deleteFlowBinder(1L, 1L);
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    private static FlowBindersBean newFlowBindersBeanWithMockedEntityManager() {
        FlowBindersBean flowBindersBean = new FlowBindersBean();
        flowBindersBean.entityManager = ENTITY_MANAGER;
        return flowBindersBean;
    }

    final long FLOW_BINDER_VERSION = 1245L;

    private FlowBinder testFlowBinder() throws JSONBException {
        FlowBinder flowBinder = new FlowBinder();
        flowBinder.setVersion(FLOW_BINDER_VERSION);
        flowBinder.setContent(new FlowBinderContentJsonBuilder().build());
        return flowBinder;
    }
}
