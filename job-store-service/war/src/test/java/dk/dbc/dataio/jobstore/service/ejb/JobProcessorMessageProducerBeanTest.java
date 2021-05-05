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

import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.util.ProcessorShard;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import org.junit.Before;
import org.junit.Test;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class JobProcessorMessageProducerBeanTest {
    private final ConnectionFactory jmsConnectionFactory = mock(ConnectionFactory.class);
    private final JMSContext jmsContext = mock(JMSContext.class);
    private final JMSProducer jmsProducer = mock(JMSProducer.class);
    private final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();

    @Before
    public void setupMocks() {
        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
    }

    @Test
    public void send_chunkArgIsNull_throws() {
        assertThat(() -> jobProcessorMessageProducerBean.send(null, new JobEntity(), Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void send_jobEntityArgIsNull_throws() {
        assertThat(() -> jobProcessorMessageProducerBean.send(new ChunkBuilder(Chunk.Type.PROCESSED).build(), null, Priority.NORMAL.getValue()),
                isThrowing(NullPointerException.class));
    }

    @Test
    public void send_setsMessagePriority() throws JobStoreException {
        jobProcessorMessageProducerBean.send(new ChunkBuilder(Chunk.Type.PARTITIONED).build(),
                buildJobEntity(), Priority.NORMAL.getValue());
        verify(jmsProducer).setPriority(Priority.NORMAL.getValue());
    }

    @Test
    public void createMessage_chunkArgIsValid_returnsMessageWithHeaderProperties() throws JSONBException, JMSException {
        final JobEntity jobEntity = buildJobEntity();
        final ProcessorShard expectedProcessorShard = new ProcessorShard(ProcessorShard.Type.ACCTEST);

        // Subject under test
        final TextMessage message = jobProcessorMessageProducerBean.createMessage(jmsContext, new ChunkBuilder(Chunk.Type.PARTITIONED).build(), jobEntity);

        // Verification
        assertThat(message.getStringProperty(JmsConstants.PAYLOAD_PROPERTY_NAME), is(JmsConstants.CHUNK_PAYLOAD_TYPE));
        assertThat(message.getStringProperty(JmsConstants.PROCESSOR_SHARD_PROPERTY_NAME), is(expectedProcessorShard.toString()));

        final FlowStoreReference flowReference = jobEntity.getFlowStoreReferences().getReference(FlowStoreReferences.Elements.FLOW);
        assertThat(message.getLongProperty(JmsConstants.FLOW_ID_PROPERTY_NAME), is(flowReference.getId()));
        assertThat(message.getLongProperty(JmsConstants.FLOW_VERSION_PROPERTY_NAME), is(flowReference.getVersion()));

        final JobSpecification jobSpecification = jobEntity.getSpecification();
        assertThat(message.getStringProperty(JmsConstants.ADDITIONAL_ARGS).contains(String.valueOf(jobSpecification.getSubmitterId())), is(true));
        assertThat(message.getStringProperty(JmsConstants.ADDITIONAL_ARGS).contains(String.valueOf(jobSpecification.getFormat())), is(true));
    }

    private JobProcessorMessageProducerBean getInitializedBean() {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = new JobProcessorMessageProducerBean();
        jobProcessorMessageProducerBean.processorQueueConnectionFactory = jmsConnectionFactory;
        jobProcessorMessageProducerBean.jsonbContext = new JSONBContext();
        return jobProcessorMessageProducerBean;
    }

    private JobEntity buildJobEntity() {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(new JobSpecification().withType(JobSpecification.Type.ACCTEST));
        jobEntity.setFlowStoreReferences(buildFlowStoreReferences());
        return jobEntity;
    }

    private FlowStoreReferences buildFlowStoreReferences() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, new FlowStoreReferenceBuilder().build());
        return flowStoreReferences;
    }
}
