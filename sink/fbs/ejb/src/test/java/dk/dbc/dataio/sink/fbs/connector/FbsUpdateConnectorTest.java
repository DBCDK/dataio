package dk.dbc.dataio.sink.fbs.connector;

import dk.dbc.dataio.sink.fbs.types.FbsUpdateConnectorException;
import dk.dbc.oss.ns.updatemarcxchange.ReasonEnum;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangePortType;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeRequest;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeResult;
import dk.dbc.oss.ns.updatemarcxchange.UpdateMarcXchangeServices;
import org.junit.Test;

import javax.jws.WebParam;
import javax.xml.ws.Binding;
import javax.xml.ws.BindingProvider;
import javax.xml.ws.EndpointReference;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class FbsUpdateConnectorTest {
    private final UpdateMarcXchangeServices services = mock(UpdateMarcXchangeServices.class);
    private final String endpoint = "http://fbs/ws";
    private final String agencyId = "agencyId";
    private final String trackingId = "trackingId";
    private final String collection =
            "<marcx:collection xmlns:marcx=\"info:lc/xmlns/marcxchange-v1\">" +
              "<marcx:record format=\"danMARC2\"><marcx:datafield ind1=\"0\" ind2=\"0\" tag=\"245\">" +
                "<marcx:subfield code=\"a\">title1</marcx:subfield></marcx:datafield>" +
              "</marcx:record>" +
            "</marcx:collection>";

    @Test(expected = NullPointerException.class)
    public void constructor1arg_endpointArgIsNull_throws() throws FbsUpdateConnectorException {
        new FbsUpdateConnector(null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor1arg_endpointArgIsEmpty_throws() throws FbsUpdateConnectorException {
        new FbsUpdateConnector("");
    }

    @Test(expected = NullPointerException.class)
    public void constructor2arg_servicesArgIsNull_throws() throws FbsUpdateConnectorException {
        new FbsUpdateConnector(null, endpoint);
    }

    @Test(expected = NullPointerException.class)
    public void constructor2arg_endpointArgIsNull_throws() throws FbsUpdateConnectorException {
        new FbsUpdateConnector(services, null);
    }

    @Test(expected = IllegalArgumentException.class)
    public void constructor2arg_endpointArgIsEmpty_throws() throws FbsUpdateConnectorException {
        new FbsUpdateConnector(services, "");
    }

    @Test
    public void updateMarcExchange_agencyIdArgIsNull_throws() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        try {
            connector.updateMarcExchange(null, collection, trackingId);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void updateMarcExchange_agencyIdArgIsEmpty_throws() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        try {
            connector.updateMarcExchange("", collection, trackingId);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void updateMarcExchange_collectionArgIsNull_throws() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        try {
            connector.updateMarcExchange(agencyId, null, trackingId);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void updateMarcExchange_collectionArgIsEmpty_throws() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        try {
            connector.updateMarcExchange(agencyId, "", trackingId);
            fail("No Exception thrown");
        } catch (IllegalArgumentException e) {
        }
    }

    @Test
    public void updateMarcExchange_collectionArgIsInvalid_throws() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        try {
            connector.updateMarcExchange(agencyId, "invalid collection", trackingId);
            fail("No Exception thrown");
        } catch (FbsUpdateConnectorException e) {
        }
    }

    @Test
    public void updateMarcExchange_allArgsAreValid_callsService() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        final MockedUpdateMarcXchangePortType proxy = getMockedUpdateMarcXchangePortType();
        final UpdateMarcXchangeResult updateMarcXchangeResult = connector.updateMarcExchange(agencyId, collection, trackingId);
        assertThat(updateMarcXchangeResult, is(notNullValue()));
        assertThat(proxy.lastRequest.getAgencyId(), is(agencyId));
        assertThat(proxy.lastRequest.getTrackingId(), is(trackingId));
        assertThat(proxy.lastRequest.getReason(), is(ReasonEnum.UPDATE_RECORD));
        assertThat(proxy.lastRequest.getMarcXchangeRecord(), is(notNullValue()));
        assertRequestContext(proxy.getRequestContext());
    }

    @Test
    public void updateMarcExchange_trackingIdIsNull_callsServiceWithoutTrackingId() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        final MockedUpdateMarcXchangePortType proxy = getMockedUpdateMarcXchangePortType();
        final UpdateMarcXchangeResult updateMarcXchangeResult = connector.updateMarcExchange(agencyId, collection, null);
        assertThat(updateMarcXchangeResult, is(notNullValue()));
        assertThat(proxy.lastRequest.getTrackingId(), is(nullValue()));
        assertRequestContext(proxy.getRequestContext());
    }

    @Test
    public void updateMarcExchange_trackingIdIsEmpty_callsServiceWithoutTrackingId() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        final MockedUpdateMarcXchangePortType proxy = getMockedUpdateMarcXchangePortType();
        final UpdateMarcXchangeResult updateMarcXchangeResult = connector.updateMarcExchange(agencyId, collection, "");
        assertThat(updateMarcXchangeResult, is(notNullValue()));
        assertThat(proxy.lastRequest.getTrackingId(), is(nullValue()));
        assertRequestContext(proxy.getRequestContext());
    }

    @Test
    public void getEndpoint_returnsEndpoint() throws FbsUpdateConnectorException {
        final FbsUpdateConnector connector = getConnector();
        assertThat(connector.getEndpoint(), is(endpoint));
    }

    private FbsUpdateConnector getConnector() throws FbsUpdateConnectorException {
        return new FbsUpdateConnector(services, endpoint);
    }

    private MockedUpdateMarcXchangePortType getMockedUpdateMarcXchangePortType() {
        final MockedUpdateMarcXchangePortType mockedUpdateMarcXchangePortType = new MockedUpdateMarcXchangePortType();
        when(services.getUpdateMarcXchangePort()).thenReturn(mockedUpdateMarcXchangePortType);
        return mockedUpdateMarcXchangePortType;
    }

    private void assertRequestContext(Map<String, Object> requestContext) {
        assertThat(requestContext.containsKey(BindingProvider.ENDPOINT_ADDRESS_PROPERTY), is(true));
        assertThat((String) requestContext.get(BindingProvider.ENDPOINT_ADDRESS_PROPERTY), is(endpoint));
        assertThat(requestContext.containsKey(FbsUpdateConnector.CONNECT_TIMEOUT_PROPERTY), is(true));
        assertThat((int) requestContext.get(FbsUpdateConnector.CONNECT_TIMEOUT_PROPERTY), is(FbsUpdateConnector.CONNECT_TIMEOUT_DEFAULT_IN_MS));
        assertThat(requestContext.containsKey(FbsUpdateConnector.REQUEST_TIMEOUT_PROPERTY), is(true));
        assertThat((int) requestContext.get(FbsUpdateConnector.REQUEST_TIMEOUT_PROPERTY), is(FbsUpdateConnector.REQUEST_TIMEOUT_DEFAULT_IN_MS));
    }

    private final class MockedUpdateMarcXchangePortType implements UpdateMarcXchangePortType, BindingProvider {
        public UpdateMarcXchangeRequest lastRequest;
        public UpdateMarcXchangeResult response = new UpdateMarcXchangeResult();
        public Map<String, Object> requestContext = new HashMap<>();

        @Override
        public Map<String, Object> getRequestContext() {
            return requestContext;
        }
        @Override
        public Map<String, Object> getResponseContext() {
            return null;
        }
        @Override
        public Binding getBinding() {
            return null;
        }
        @Override
        public EndpointReference getEndpointReference() {
            return null;
        }
        @Override
        public <T extends EndpointReference> T getEndpointReference(Class<T> clazz) {
            return null;
        }
        @Override
        public UpdateMarcXchangeResult updateMarcXchange(
                @WebParam(name = "updateMarcXchangeRequest", targetNamespace = "http://oss.dbc.dk/ns/updateMarcXchange")
                UpdateMarcXchangeRequest updateMarcXchangeRequest) {
            lastRequest = updateMarcXchangeRequest;
            return response;
        }
    }
}