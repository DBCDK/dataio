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

package dk.dbc.dataio.jobstore.service.ejb;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsMessageDrivenContext;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import javax.ejb.MessageDrivenContext;
import javax.jms.JMSException;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

public class DmqMessageConsumerBeanTest {
    private TestableDmqMessageConsumerBean dmqMessageConsumerBean;
    private Map<String, Object> headers;

    @Before
    public void setup() {
        headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        initializeDmqMessageConsumerBean();
    }

    @Test
    public void onMessage_messageArgPayloadIsInvalid_noTransactionRollback() throws JMSException, JobStoreException {
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        textMessage.setText("{'invalid': 'instance'}");
        dmqMessageConsumerBean.onMessage(textMessage);
        assertThat(dmqMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));
        verify(dmqMessageConsumerBean.jobStoreBean, times(0)).addChunk(any(Chunk.class));
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsInvalid_throws() throws JobStoreException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", headers, "{'invalid': 'instance'}");
        dmqMessageConsumerBean.handleConsumedMessage(consumedMessage);
    }

    @Test(expected = InvalidMessageException.class)
    public void handleConsumedMessage_messageArgPayloadIsUnknown_throws() throws JobStoreException, InvalidMessageException {
        final ConsumedMessage consumedMessage = new ConsumedMessage("id", Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, "Unknown"), "{'unknown': 'instance'}");
        dmqMessageConsumerBean.handleConsumedMessage(consumedMessage);
    }

    @Test
    public void onMessage_deadPartitionedChunk_singleChunkAdded() throws JMSException, JobStoreException, JSONBException {
        final Chunk originalChunk = new ChunkBuilder(Chunk.Type.PARTITIONED).build();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        textMessage.setText(dmqMessageConsumerBean.jsonbContext.marshall(originalChunk));
        dmqMessageConsumerBean.onMessage(textMessage);
        assertThat(dmqMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));

        final ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(dmqMessageConsumerBean.jobStoreBean).addChunk(chunkCaptor.capture());
        final List<Chunk> capturedChunks = chunkCaptor.getAllValues();
        assertThat("1st dead chunk size", capturedChunks.get(0).size(), is(originalChunk.size()));
        assertThat("1st dead chunk type", capturedChunks.get(0).getType(), is(Chunk.Type.PROCESSED));

        verify(dmqMessageConsumerBean.jobSchedulerBean).chunkProcessingDone(capturedChunks.get(0));
    }

    @Test
    public void onMessage_deadProcessedChunk_singleChunkAdded() throws JMSException, JobStoreException, JSONBException {
        final Chunk originalChunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final MockedJmsTextMessage textMessage = new MockedJmsTextMessage();
        textMessage.setStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);
        textMessage.setText(dmqMessageConsumerBean.jsonbContext.marshall(originalChunk));
        dmqMessageConsumerBean.onMessage(textMessage);
        assertThat(dmqMessageConsumerBean.getMessageDrivenContext().getRollbackOnly(), is(false));

        verify(dmqMessageConsumerBean.jobSchedulerBean).chunkDeliveringDone(any(Chunk.class));

        final ArgumentCaptor<Chunk> chunkCaptor = ArgumentCaptor.forClass(Chunk.class);
        verify(dmqMessageConsumerBean.jobStoreBean).addChunk(chunkCaptor.capture());
        final List<Chunk> capturedChunks = chunkCaptor.getAllValues();
        assertThat("dead chunk size", capturedChunks.get(0).size(), is(originalChunk.size()));
        assertThat("dead chunk type", capturedChunks.get(0).getType(), is(Chunk.Type.DELIVERED));

        verify(dmqMessageConsumerBean.jobSchedulerBean).chunkDeliveringDone(capturedChunks.get(0));
    }

    private static class TestableDmqMessageConsumerBean extends DmqMessageConsumerBean {
        public MessageDrivenContext getMessageDrivenContext() {
            return messageDrivenContext;
        }
        public void setMessageDrivenContext(MessageDrivenContext messageDrivenContext) {
            this.messageDrivenContext = messageDrivenContext;
        }
    }

    private void initializeDmqMessageConsumerBean() {
        dmqMessageConsumerBean = new TestableDmqMessageConsumerBean();
        dmqMessageConsumerBean.setMessageDrivenContext(new MockedJmsMessageDrivenContext());
        dmqMessageConsumerBean.jobStoreBean = mock(PgJobStore.class);
        dmqMessageConsumerBean.jobSchedulerBean = mock(JobSchedulerBean.class);
    }
}
