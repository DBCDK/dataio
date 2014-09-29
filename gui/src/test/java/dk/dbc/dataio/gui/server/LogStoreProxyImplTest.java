package dk.dbc.dataio.gui.server;

import dk.dbc.dataio.commons.utils.httpclient.HttpClient;
import dk.dbc.dataio.commons.utils.service.ServiceUtil;
import dk.dbc.dataio.gui.client.exceptions.ProxyError;
import dk.dbc.dataio.gui.client.exceptions.ProxyException;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnector;
import dk.dbc.dataio.logstore.service.connector.LogStoreServiceConnectorUnexpectedStatusCodeException;
import org.glassfish.jersey.client.ClientConfig;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

import javax.naming.NamingException;
import javax.ws.rs.client.Client;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.hamcrest.core.Is.is;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.mockStatic;
import static org.powermock.api.mockito.PowerMockito.when;

@RunWith(PowerMockRunner.class)
@PrepareForTest({
        HttpClient.class,
        ServiceUtil.class
})
public class LogStoreProxyImplTest {
    private final String logStoreServiceUrl;
    private final Client client = mock(Client.class);

    private static final String JOB_ID = "jobId";
    private static final long CHUNK_ID = 5L;
    private static final long ITEM_ID = 434;

    public LogStoreProxyImplTest() {
        logStoreServiceUrl = "http://dataio/log-store";
    }

    @Before
    public void setup() throws Exception {
        mockStatic(ServiceUtil.class);
        mockStatic(HttpClient.class);
        when(ServiceUtil.getLogStoreServiceEndpoint()).thenReturn(logStoreServiceUrl);
        when(HttpClient.newClient(any(ClientConfig.class))).thenReturn(client);
    }

    @Test
    public void noArgs_logStoreProxyConstructorLogStoreService_EndpointCanNotBeLookedUp_throws() throws Exception{
        when(ServiceUtil.getLogStoreServiceEndpoint()).thenThrow(new NamingException());
        try{
            new LogStoreProxyImpl();
            fail();
        }catch (NamingException e){
        }
    }

    @Test
    public void oneArg_logStoreProxyConstructorLogStoreService_EndpointCanNotBeLookedUp_throws1() throws Exception{
        final LogStoreServiceConnector logStoreServiceConnector = mock(LogStoreServiceConnector.class);
        when(ServiceUtil.getLogStoreServiceEndpoint()).thenThrow(new NamingException());
        try{
            new LogStoreProxyImpl(logStoreServiceConnector);
            fail();
        }catch (NamingException e){
        }
    }

    /*
    * Test getItemLog
    */

    @Test
    public void getItemLog_remoteServiceReturnsHttpStatusOk_returnsLogAsString() throws Exception {
        final LogStoreServiceConnector logStoreServiceConnector = mock(LogStoreServiceConnector.class);
        final LogStoreProxyImpl logStoreProxy = new LogStoreProxyImpl(logStoreServiceConnector);
        String log = "\t something something \n";

        when(logStoreServiceConnector.getItemLog(eq(JOB_ID), eq(CHUNK_ID), eq(ITEM_ID))).thenReturn(log);

        try {
            log = logStoreProxy.getItemLog(JOB_ID, CHUNK_ID, ITEM_ID);
            assertThat(log, not(nullValue()));
        } catch (ProxyException e) {
            fail("Unexpected error when calling: getItemLog()");
        }
    }

    @Test
    public void getItemLog_remoteServiceReturnsHttpStatusInternalServerError_throws() throws Exception {
        getItemLog_genericTestImplForHttpErrors(500, ProxyError.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR");
    }

    @Test
    public void getItemLog_remoteServiceReturnsHttpStatusNotFound_throws() throws Exception {
        getItemLog_genericTestImplForHttpErrors(404, ProxyError.ENTITY_NOT_FOUND, "ENTITY_NOT_FOUND");
    }

    @Test
    public void getItemLog_nullValuedJobId_throws() throws Exception {
        getItemLog_GenericTestImplForJobIdValidationErrors(null, ProxyError.BAD_REQUEST, "BAD_REQUEST");
    }

    @Test
    public void getItemLog_emptyValuedJobId_throws() throws Exception{
        getItemLog_GenericTestImplForJobIdValidationErrors("", ProxyError.BAD_REQUEST, "BAD_REQUEST");
    }

    private void getItemLog_GenericTestImplForJobIdValidationErrors(String jobId, ProxyError expectedError, String expectedErrorName) throws Exception {
        final LogStoreServiceConnector logStoreServiceConnector = mock(LogStoreServiceConnector.class);
        final LogStoreProxyImpl logStoreProxy = new LogStoreProxyImpl(logStoreServiceConnector);
        when(logStoreServiceConnector.getItemLog(eq(jobId), eq(CHUNK_ID), eq(ITEM_ID))).thenThrow(new IllegalArgumentException());

        try {
            logStoreProxy.getItemLog(jobId, CHUNK_ID, ITEM_ID);
            fail("No " + expectedErrorName + " error was thrown by getItemLog()");
        } catch(ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }

    private void getItemLog_genericTestImplForHttpErrors(int errorCodeToReturn, ProxyError expectedError, String expectedErrorName) throws Exception {
        final LogStoreServiceConnector logStoreServiceConnector = mock(LogStoreServiceConnector.class);
        final LogStoreProxyImpl logStoreProxy = new LogStoreProxyImpl(logStoreServiceConnector);
        when(logStoreServiceConnector.getItemLog(eq(JOB_ID), eq(CHUNK_ID), eq(ITEM_ID))).thenThrow(new LogStoreServiceConnectorUnexpectedStatusCodeException("DIED", errorCodeToReturn));

        try {
            logStoreProxy.getItemLog(JOB_ID, CHUNK_ID, ITEM_ID);
            fail("No " + expectedErrorName + " error was thrown by getItemLog()");
        } catch (ProxyException e) {
            assertThat(e.getErrorCode(), is(expectedError));
        }
    }
}
