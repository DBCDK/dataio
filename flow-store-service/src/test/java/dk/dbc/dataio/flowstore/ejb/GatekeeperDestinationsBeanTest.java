package dk.dbc.dataio.flowstore.ejb;

import com.fasterxml.jackson.databind.node.ArrayNode;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.GatekeeperDestination;
import dk.dbc.dataio.commons.utils.test.json.GatekeeperDestinationJsonBuilder;
import dk.dbc.dataio.commons.utils.test.model.GatekeeperDestinationBuilder;
import dk.dbc.dataio.flowstore.entity.GatekeeperDestinationEntity;
import jakarta.persistence.EntityManager;
import jakarta.persistence.Query;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.core.UriBuilder;
import jakarta.ws.rs.core.UriInfo;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.net.URI;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class GatekeeperDestinationsBeanTest {

    private static final EntityManager ENTITY_MANAGER = mock(EntityManager.class);
    private UriInfo mockedUriInfo;
    private JSONBContext jsonbContext;

    @BeforeEach
    public void setup() throws URISyntaxException {
        mockedUriInfo = mock(UriInfo.class);
        UriBuilder mockedUriBuilder = mock(UriBuilder.class);

        when(mockedUriInfo.getAbsolutePathBuilder()).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.path(anyString())).thenReturn(mockedUriBuilder);
        when(mockedUriBuilder.build()).thenReturn(new URI("location"));

        jsonbContext = new JSONBContext();
    }

    // ***************************************************** create gatekeeper destination ******************************************************

    @Test
    public void gatekeeperDestinationsBean_validConstructor_newInstance() {
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        assertThat(gatekeeperDestinationsBean, is(notNullValue()));
    }

    @Test
    public void createGatekeeperDestination_nullGatekeeperDestination_throws() {
        assertThrows(NullPointerException.class, () -> newGatekeeperDestinationsBeanWithMockedEntityManager().createGatekeeperDestination(mockedUriInfo, null));
    }

    @Test
    public void createGatekeeperDestination_emptyGatekeeperDestination_throws() {
        assertThrows(IllegalArgumentException.class, () -> newGatekeeperDestinationsBeanWithMockedEntityManager().createGatekeeperDestination(mockedUriInfo, ""));
    }

    @Test
    public void createGatekeeperDestination_invalidJSON_throwsJsonException() {
        assertThrows(JSONBException.class, () -> newGatekeeperDestinationsBeanWithMockedEntityManager().createGatekeeperDestination(mockedUriInfo, "invalid Json"));
    }

    @Test
    public void createGatekeeperDestination_gatekeeperDestinationCreated_returnsResponseWithHttpStatusOk() throws JSONBException {
        String gatekeeperDestination = new GatekeeperDestinationJsonBuilder().build();
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();

        // Subject under test
        Response response = gatekeeperDestinationsBean.createGatekeeperDestination(mockedUriInfo, gatekeeperDestination);

        // Verification
        assertThat(response.getStatus(), is(Response.Status.CREATED.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    // **************************************************** find all gatekeeper destinations ****************************************************

    @Test
    public void findAllGatekeeperDestinations_noGatekeeperDestinationsFound_returnsResponseWithHttpStatusOkAndEmptyList() throws JSONBException {
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        Query query = mock(Query.class);

        when(ENTITY_MANAGER.createNamedQuery(GatekeeperDestinationEntity.QUERY_FIND_ALL)).thenReturn(query);
        when(query.getResultList()).thenReturn(Collections.emptyList());

        // Subject under test
        Response response = gatekeeperDestinationsBean.findAllGatekeeperDestinations();

        // Verification
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(0));
    }

    @Test
    public void findAllGatekeeperDestinations_gatekeeperDestinationsFound_returnsResponseWithHttpStatusOkAndGatekeeperDestinationEntities() throws JSONBException {
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        Query query = mock(Query.class);
        GatekeeperDestinationEntity gatekeeperDestinationEntityA = jsonbContext.unmarshall(
                new GatekeeperDestinationJsonBuilder().setSubmitterNumber("123").build(), GatekeeperDestinationEntity.class);

        GatekeeperDestinationEntity gatekeeperDestinationEntityB = jsonbContext.unmarshall(
                new GatekeeperDestinationJsonBuilder().setSubmitterNumber("234").build(), GatekeeperDestinationEntity.class);

        when(ENTITY_MANAGER.createNamedQuery(GatekeeperDestinationEntity.QUERY_FIND_ALL)).thenReturn(query);
        when(query.getResultList()).thenReturn(Arrays.asList(gatekeeperDestinationEntityA, gatekeeperDestinationEntityB));

        // Subject under test
        Response response = gatekeeperDestinationsBean.findAllGatekeeperDestinations();

        // Verification
        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
        ArrayNode entityNode = (ArrayNode) jsonbContext.getJsonTree((String) response.getEntity());
        assertThat(entityNode.size(), is(2));
        assertThat(entityNode.get(0).get("submitterNumber").textValue(), is("123"));
        assertThat(entityNode.get(1).get("submitterNumber").textValue(), is("234"));
    }

    // ****************************************************** delete gatekeeper destination *****************************************************

    @Test
    public void deleteGatekeeperDestination_gatekeeperDestinationNotFound_returnsResponseWithHttpStatusNotFound() {
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(null);

        // Subject under test
        Response response = gatekeeperDestinationsBean.deleteGatekeeperDestination(42L);

        // Verification
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void deleteGatekeeperDestination_gatekeeperDestinationFound_returnsNoContentHttpResponse() {
        GatekeeperDestinationEntity gatekeeperDestinationEntity = mock(GatekeeperDestinationEntity.class);
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(gatekeeperDestinationEntity);

        // Subject under test
        Response response = gatekeeperDestinationsBean.deleteGatekeeperDestination(42L);

        // Verification
        verify(ENTITY_MANAGER).remove(gatekeeperDestinationEntity);
        assertThat(response.getStatus(), is(Response.Status.NO_CONTENT.getStatusCode()));
    }

    // ***************************************************** update gatekeeper destination ******************************************************

    @Test
    public void updateGatekeeperDestination_nullGatekeeperDestination_throws() {
        assertThrows(NullPointerException.class, () -> newGatekeeperDestinationsBeanWithMockedEntityManager().updateGatekeeperDestination(null, 42L));
    }

    @Test
    public void updateGatekeeperDestination_emptyGatekeeperDestination_throws() {
        assertThrows(IllegalArgumentException.class, () -> newGatekeeperDestinationsBeanWithMockedEntityManager().updateGatekeeperDestination("", 42L));
    }

    @Test
    public void updateGatekeeperDestination_gatekeeperDestinationNotFound_returnsResponseWithHttpStatusNotFound() throws JSONBException {
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();

        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(null);

        // Subject under test
        Response response = gatekeeperDestinationsBean.updateGatekeeperDestination(new GatekeeperDestinationJsonBuilder().build(), 42L);

        // Verification
        assertThat(response.getStatus(), is(Response.Status.NOT_FOUND.getStatusCode()));
    }

    @Test
    public void updateGatekeeperDestination_gatekeeperDestinationFound_returnsResponseWithHttpStatusOk() throws JSONBException {
        GatekeeperDestinationEntity gatekeeperDestinationEntity = mock(GatekeeperDestinationEntity.class);
        GatekeeperDestination gatekeeperDestination = new GatekeeperDestinationBuilder().build();
        GatekeeperDestinationsBean gatekeeperDestinationsBean = newGatekeeperDestinationsBeanWithMockedEntityManager();
        gatekeeperDestinationsBean.jsonbContext = mock(JSONBContext.class);

        when(gatekeeperDestinationsBean.jsonbContext.unmarshall(anyString(), eq(GatekeeperDestination.class))).thenReturn(gatekeeperDestination);
        when(ENTITY_MANAGER.find(eq(GatekeeperDestinationEntity.class), any())).thenReturn(gatekeeperDestinationEntity);
        when(gatekeeperDestinationsBean.jsonbContext.marshall(gatekeeperDestinationEntity)).thenReturn("entity");

        // Subject under test
        Response response = gatekeeperDestinationsBean.updateGatekeeperDestination(
                new JSONBContext().marshall(gatekeeperDestination), gatekeeperDestination.getId());

        // Verification
        verify(gatekeeperDestinationEntity).setSubmitterNumber(gatekeeperDestination.getSubmitterNumber());
        verify(gatekeeperDestinationEntity).setDestination(gatekeeperDestination.getDestination());
        verify(gatekeeperDestinationEntity).setPackaging(gatekeeperDestination.getPackaging());
        verify(gatekeeperDestinationEntity).setFormat(gatekeeperDestination.getFormat());

        assertThat(response.getStatus(), is(Response.Status.OK.getStatusCode()));
        assertThat(response.hasEntity(), is(true));
    }

    /*
     * Private methods
     */

    private static GatekeeperDestinationsBean newGatekeeperDestinationsBeanWithMockedEntityManager() {
        GatekeeperDestinationsBean gatekeeperDestinationsBean = new GatekeeperDestinationsBean();
        gatekeeperDestinationsBean.entityManager = ENTITY_MANAGER;
        return gatekeeperDestinationsBean;
    }
}
