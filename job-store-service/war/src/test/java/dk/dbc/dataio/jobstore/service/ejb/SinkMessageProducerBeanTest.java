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

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.entity.SinkCacheEntity;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SinkMessageProducerBeanTest {
    /* mocks */
    private ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private JMSContext jmsContext = mock(JMSContext.class);
    private JMSProducer jmsProducer = mock(JMSProducer.class);
    private SinkCacheEntity sinkCacheEntity = mock(SinkCacheEntity.class);

    private Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
    private JobEntity jobEntity = new JobEntity();
    private Sink sink = new SinkBuilder().build();
    private FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
    {
        jobEntity.setCachedSink(sinkCacheEntity);
        jobEntity.setFlowStoreReferences(flowStoreReferences);
        flowStoreReferences.setReference(FlowStoreReferences.Elements.SINK,
                new FlowStoreReference(sink.getId(), sink.getVersion(), sink.getContent().getName()));
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW_BINDER,
                new FlowStoreReference(42, 1, "test-binder"));
    }

    @Before
    public void setupExpectations() {
        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);

        when(sinkCacheEntity.getSink()).thenReturn(sink);
    }

    @Test (expected = NullPointerException.class)
    public void send_chunkArgIsNull_throws() throws JobStoreException {
        // Subject Under Test
        getInitializedBean().send(null, jobEntity);
    }

    @Test (expected = NullPointerException.class)
    public void send_jobEntityArgIsNull_throws() throws JobStoreException {
        // Subject Under Test
        getInitializedBean().send(chunk, null);
    }

    @Test (expected = JobStoreException.class)
    public void send_createMessageThrowsJsonException_throws() throws JobStoreException, JSONBException {
        final JSONBContext jsonbContext = mock(JSONBContext.class);
        when(jsonbContext.marshall(anyObject())).thenThrow(new JSONBException("JsonException"));
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        sinkMessageProducerBean.jsonbContext = jsonbContext;

        // Subject Under Test
        sinkMessageProducerBean.send(chunk, jobEntity);
    }

    @Test
    public void createMessage_chunkArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JSONBException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();

        // Subject Under Test
        final TextMessage message = sinkMessageProducerBean.createMessage(jmsContext, chunk, sink, flowStoreReferences);

        // Verifications
        final FlowStoreReference sinkReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.SINK);
        final FlowStoreReference flowBinderReference = flowStoreReferences.getReference(FlowStoreReferences.Elements.FLOW_BINDER);
        assertThat("Message payload property", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
        assertThat("Message resource property", message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(sink.getContent().getResource()));
        assertThat("Message id property", message.getLongProperty(JmsConstants.SINK_ID_PROPERTY_NAME), is(sinkReference.getId()));
        assertThat("Message version property", message.getLongProperty(JmsConstants.SINK_VERSION_PROPERTY_NAME), is(sinkReference.getVersion()));
        assertThat("Message flowBinderId property", message.getLongProperty(JmsConstants.FLOW_BINDER_ID_PROPERTY_NAME), is(flowBinderReference.getId()));
        assertThat("Message flowBinderVersion property", message.getLongProperty(JmsConstants.FLOW_BINDER_VERSION_PROPERTY_NAME), is(flowBinderReference.getVersion()));
    }

    private SinkMessageProducerBean getInitializedBean() {
        final SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.sinksQueueConnectionFactory = jmsConnectionFactory;
        return sinkMessageProducerBean;
    }
}
