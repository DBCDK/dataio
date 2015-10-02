/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

package dk.dbc.dataio.jobprocessor.ejb;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Flow;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SupplementaryProcessDataBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
import dk.dbc.dataio.jobstore.types.ResourceBundle;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import java.util.Arrays;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobStoreMessageConsumerBeanTest {
    private JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalidNewJob_noTransactionRollback() throws JMSException {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        jobStoreMessageConsumerBean.onMessage(textMessage);
        assertThat("RollbackOnly", jobStoreMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
    }

    @Test
    public void handleConsumedMessage_messageArgPayloadIsInvalidNewJob_throws() throws JobProcessorException, JMSException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, "{'invalid': 'instance'}");
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        try {
            jobStoreMessageConsumerBean.handleConsumedMessage(consumedMessage);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }

    @Test
    public void handleConsumedMessage_messagePayloadCanNotBeUnmarshalledToJson_throws() throws JobProcessorException, InvalidMessageException {
        final ConsumedMessage message = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, "invalid");
        final JobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        try {
            jobStoreMessageConsumerBean.handleConsumedMessage(message);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }

    @Test
    public void handleConsumedMessage_messageChunkIsOfIncorrectType_throws() throws JobProcessorException, InvalidMessageException, JMSException, JSONBException {
        final ChunkItem item = new ChunkItemBuilder().setData(StringUtil.asBytes("This is some data")).setStatus(ChunkItem.Status.SUCCESS).build();
        // The Chunk-type 'processed' is not allowed in the JobProcessor, only 'partitioned' is allowed.
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).setItems(Arrays.asList(item)).build();
        final String jsonChunk = new JSONBContext().marshall(chunk);

        final JobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final ConsumedMessage message = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, jsonChunk);
        try {
            jobStoreMessageConsumerBean.handleConsumedMessage(message);
            fail("No exception thrown");
        } catch (InvalidMessageException e) {
        }
    }

    @Test
    public void handleConsumedMessage_happyPath() throws Exception {

        final ChunkProcessorBeanTest jsFactory = new ChunkProcessorBeanTest();
        final Flow flow = jsFactory.getFlow(new ChunkProcessorBeanTest.ScriptWrapper(jsFactory.javaScriptReturnUpperCase,
                jsFactory.getJavaScript(jsFactory.getJavaScriptReturnUpperCaseFunction())));
        final Sink sink = new SinkBuilder().build();

        final ResourceBundle resourceBundle = new ResourceBundle(flow, sink, new SupplementaryProcessDataBuilder().build());
        when(jobStoreServiceConnector.getResourceBundle(anyInt())).thenReturn(resourceBundle);

        final ChunkItem item = new ChunkItemBuilder().setData(StringUtil.asBytes("This is some data")).setStatus(ChunkItem.Status.SUCCESS).build();
        final ExternalChunk chunk = new ExternalChunkBuilder(ExternalChunk.Type.PARTITIONED).setItems(Arrays.asList(item)).build();
        final String jsonChunk = new JSONBContext().marshall(chunk);

        final JobStoreMessageConsumerBean jobStoreMessageConsumerBean = getInitializedBean();
        final ConsumedMessage message = new ConsumedMessage("id", JmsConstants.CHUNK_PAYLOAD_TYPE, jsonChunk);
        jobStoreMessageConsumerBean.handleConsumedMessage(message);

        // This is called when the processor has processed the data.
        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(ExternalChunk.class), anyLong(), anyLong());
    }

    private TestableJobStoreMessageConsumerBean getInitializedBean() {
        final TestableJobStoreMessageConsumerBean jobStoreMessageConsumerBean = new TestableJobStoreMessageConsumerBean();
        jobStoreMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        jobStoreMessageConsumerBean.jobStoreServiceConnector = jobStoreServiceConnectorBean;
        jobStoreMessageConsumerBean.chunkProcessor = new ChunkProcessorBean();
        return jobStoreMessageConsumerBean;
    }

    private static class TestableJobStoreMessageConsumerBean extends JobStoreMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
    }
}


