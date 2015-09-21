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

import dk.dbc.dataio.commons.types.ExternalChunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ExternalChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.jobprocessor.exception.JobProcessorException;
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
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyObject;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class SinkMessageProducerBeanTest {
    private ConnectionFactory jmsConnectionFactory;
    private JMSContext jmsContext;
    private JMSProducer jmsProducer;
    private ExternalChunk processedChunk = new ExternalChunkBuilder(ExternalChunk.Type.PROCESSED).build();
    private Sink sink = new SinkBuilder().build();

    @Before
    public void setup() {
        jmsConnectionFactory = mock(ConnectionFactory.class);
        jmsContext = mock(JMSContext.class);
        jmsProducer = mock(JMSProducer.class);

        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @Test
    public void send_processedChunkArgIsNull_throws() throws JobProcessorException {
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        try {
            sinkMessageProducerBean.send(null, sink);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void send_destinationArgIsNull_throws() throws JobProcessorException {
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        try {
            sinkMessageProducerBean.send(processedChunk, null);
            fail("No Exception thrown");
        } catch (NullPointerException e) {
        }
    }

    @Test
    public void send_createMessageThrowsJsonException_throws() throws JobProcessorException, JSONBException {
        final JSONBContext jsonbContext = mock(JSONBContext.class);
        when(jsonbContext.marshall(anyObject())).thenThrow(new JSONBException("JsonException"));
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        sinkMessageProducerBean.jsonbContext = jsonbContext;
        try {
            sinkMessageProducerBean.send(processedChunk, sink);
            fail("No Exception thrown");
        } catch (JobProcessorException e) {
        }
    }

    @Test
    public void createMessage_deliveredChunkArgIsValid_returnsMessageWithHeaderProperties() throws JMSException, JSONBException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final SinkMessageProducerBean sinkMessageProducerBean = getInitializedBean();
        final TextMessage message = sinkMessageProducerBean.createMessage(jmsContext, processedChunk, sink);
        assertThat("Message source property", message.getStringProperty(JmsConstants.SOURCE_PROPERTY_NAME), is(JmsConstants.PROCESSOR_SOURCE_VALUE));
        assertThat("Message payload property", message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
        assertThat("Message resource property", message.getStringProperty(JmsConstants.RESOURCE_PROPERTY_NAME), is(sink.getContent().getResource()));
    }

    private SinkMessageProducerBean getInitializedBean() {
        final SinkMessageProducerBean sinkMessageProducerBean = new SinkMessageProducerBean();
        sinkMessageProducerBean.sinksQueueConnectionFactory = jmsConnectionFactory;
        return sinkMessageProducerBean;
    }
}
