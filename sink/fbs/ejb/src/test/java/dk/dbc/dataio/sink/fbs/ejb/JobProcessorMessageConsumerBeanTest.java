package dk.dbc.dataio.sink.fbs.ejb;

import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.json.JsonException;
import dk.dbc.dataio.commons.utils.json.JsonUtil;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.EJBException;
import javax.xml.ws.WebServiceException;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobProcessorMessageConsumerBeanTest {
    private static final String MESSAGE_ID = "id";
    private static final String PAYLOAD_TYPE = JmsConstants.CHUNK_PAYLOAD_TYPE;
    private String PAYLOAD;

    private final FbsPusherBean fbsPusherBean = mock(FbsPusherBean.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Before
    public void setup() throws JsonException {
        PAYLOAD = JsonUtil.toJson(new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build());
    }

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_consumedMessageArgContainsInvalidPayload_throws() throws InvalidMessageException, SinkException {
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, "payload");
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    @Test
    public void handleConsumedMessage_pusherThrowsWebServiceException_throws() throws InvalidMessageException, SinkException {
        final WebServiceException webServiceException = new WebServiceException("died");
        when(fbsPusherBean.push(any(ExternalChunk.class))).thenThrow(webServiceException);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        try {
            getInitializedBean().handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (WebServiceException e) {
            assertThat(e, is(webServiceException));
        }
    }

    @Test
    public void handleConsumedMessage_addChunkIgnoreDuplicatesJobStoreServiceConnectorException_throwsEJBException() throws InvalidMessageException, JobStoreServiceConnectorException {
        JobStoreServiceConnectorException jobStoreServiceConnectorException = new JobStoreServiceConnectorException("DIED");
        final ExternalChunk deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build();
        when(fbsPusherBean.push(any(ExternalChunk.class))).thenReturn(deliveredChunk);
        doThrow(jobStoreServiceConnectorException).when(jobStoreServiceConnector).addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId());
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        try {
            getInitializedBean().handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (EJBException e) {
            assertThat(e.getMessage().contains(jobStoreServiceConnectorException.getMessage()), is(true));
        }
    }

    @Test
    public void handleConsumedMessage_addChunkIgnoreDuplicates_ok() throws InvalidMessageException, JobStoreServiceConnectorException {
        final ExternalChunk deliveredChunk = new ExternalChunkBuilder(ExternalChunk.Type.DELIVERED).build();
        when(fbsPusherBean.push(any(ExternalChunk.class))).thenReturn(deliveredChunk);
        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(deliveredChunk, deliveredChunk.getJobId(), deliveredChunk.getChunkId())).thenReturn(null);
        final ConsumedMessage consumedMessage = new ConsumedMessage(MESSAGE_ID, PAYLOAD_TYPE, PAYLOAD);
        getInitializedBean().handleConsumedMessage(consumedMessage);
    }

    private JobProcessorMessageConsumerBean getInitializedBean() {
        final JobProcessorMessageConsumerBean jobProcessorMessageConsumerBean = new JobProcessorMessageConsumerBean();
        jobProcessorMessageConsumerBean.fbsPusher = fbsPusherBean;
        jobProcessorMessageConsumerBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return jobProcessorMessageConsumerBean;
    }
}