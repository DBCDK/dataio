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
import dk.dbc.dataio.commons.types.JobSpecification;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.test.jms.MockedJmsTextMessage;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.JobSpecificationBuilder;
import dk.dbc.dataio.jobstore.service.entity.JobEntity;
import dk.dbc.dataio.jobstore.service.util.ProcessorShard;
import dk.dbc.dataio.jobstore.test.types.FlowStoreReferenceBuilder;
import dk.dbc.dataio.jobstore.types.FlowStoreReference;
import dk.dbc.dataio.jobstore.types.FlowStoreReferences;
import dk.dbc.dataio.jobstore.types.JobStoreException;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import javax.jms.ConnectionFactory;
import javax.jms.JMSContext;
import javax.jms.JMSException;
import javax.jms.JMSProducer;
import javax.jms.TextMessage;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class JobProcessorMessageProducerBeanTest {
    private ConnectionFactory jmsConnectionFactory;
    private JMSContext jmsContext;
    private JMSProducer jmsProducer;
    private JSONBContext jsonbContext;

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Before
    public void setup() {
        jmsConnectionFactory = mock(ConnectionFactory.class);
        jmsContext = mock(JMSContext.class);
        jmsProducer = mock(JMSProducer.class);
        jsonbContext = mock(JSONBContext.class);

        when(jmsConnectionFactory.createContext()).thenReturn(jmsContext);
        when(jmsContext.createProducer()).thenReturn(jmsProducer);
    }

    @Test
    public void send_chunkArgIsNull_throws() {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        assertThat(() -> jobProcessorMessageProducerBean.send(null, new JobEntity()), isThrowing(NullPointerException.class));
    }

    @Test
    public void send_jobEntityArgIsNull_throws() {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        assertThat(() -> jobProcessorMessageProducerBean.send(new ChunkBuilder(Chunk.Type.PROCESSED).build(), null), isThrowing(NullPointerException.class));
    }


    @Test
    public void send_createMessageThrowsJsonException_throws() throws JobStoreException, JSONBException {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
        when(jobProcessorMessageProducerBean.jsonbContext.marshall(any(Chunk.class))).thenThrow(new JSONBException("DIED"));
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(new JobSpecificationBuilder().build());

        // Subject under test
        assertThat(() -> jobProcessorMessageProducerBean.send(
                new ChunkBuilder(Chunk.Type.PARTITIONED).build(),
                jobEntity),
                isThrowing(JobStoreException.class));
    }

    @Test
    public void createMessage_chunkArgIsValid_returnsMessageWithHeaderProperties() throws JSONBException, JMSException {
        when(jmsContext.createTextMessage(any(String.class))).thenReturn(new MockedJmsTextMessage());
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = getInitializedBean();
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


    /*
     * Private methods
     */
    private JobProcessorMessageProducerBean getInitializedBean() {
        final JobProcessorMessageProducerBean jobProcessorMessageProducerBean = new JobProcessorMessageProducerBean();
        jobProcessorMessageProducerBean.processorQueueConnectionFactory = jmsConnectionFactory;
        jobProcessorMessageProducerBean.jsonbContext = jsonbContext;
        return jobProcessorMessageProducerBean;
    }

    private JobEntity buildJobEntity() {
        final JobEntity jobEntity = new JobEntity();
        jobEntity.setSpecification(new JobSpecificationBuilder().setType(JobSpecification.Type.ACCTEST).build());
        jobEntity.setFlowStoreReferences(buildFlowStoreReferences());
        return jobEntity;
    }

    private FlowStoreReferences buildFlowStoreReferences() {
        FlowStoreReferences flowStoreReferences = new FlowStoreReferences();
        flowStoreReferences.setReference(FlowStoreReferences.Elements.FLOW, new FlowStoreReferenceBuilder().build());
        return flowStoreReferences;
    }
}
