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

package dk.dbc.dataio.sink.ims;

import com.github.tomakehurst.wiremock.junit.WireMockRule;
import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.metricshandler.CounterMetric;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.commons.metricshandler.SimpleTimerMetric;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.ImsSinkConfig;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.ims.connector.ImsServiceConnectorTest;
import dk.dbc.dataio.sink.types.SinkException;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ImsMessageProcessorBeanTest {
    @Rule  // Port 0 lets wiremock find a random port
    public WireMockRule wireMockRule = new WireMockRule(0);

    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private final ImsConfigBean imsConfigBean = mock(ImsConfigBean.class);
    private final JSONBContext jsonbContext = new JSONBContext();
    private final MetricsHandlerBean metricsHandler = mock(MetricsHandlerBean.class);

    @Before
    public void setupMocks() throws SinkException {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
        when(imsConfigBean.getConfig(any(ConsumedMessage.class))).thenReturn(new ImsSinkConfig()
                .withEndpoint(getWireMockEndpoint()));
        doNothing().when(metricsHandler).increment(any(CounterMetric.class), any());
        doNothing().when(metricsHandler).update(any(SimpleTimerMetric.class), any());
    }

    @Test
    public void handleConsumedMessage_jobStoreCommunicationFails_throws() throws InvalidMessageException, JobStoreServiceConnectorException {
        final JobStoreServiceConnectorException jobStoreServiceConnectorException = new JobStoreServiceConnectorException("Exception from job-store");
        when(jobStoreServiceConnector.addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong()))
                .thenThrow(jobStoreServiceConnectorException);

        try {
            imsMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(getIgnoredChunk()));
            fail("No SinkException thrown");
        } catch (SinkException e) {
            assertThat(e.getCause(), is(jobStoreServiceConnectorException));
        }
    }

    @Test
    public void handleConsumedMessage() throws InvalidMessageException, SinkException {
        final ImsServiceConnectorTest.MarcXchangeRecordsTwoOkOneFail requestResponse = new ImsServiceConnectorTest.MarcXchangeRecordsTwoOkOneFail();
        requestResponse.stub();
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(42).setChunkId(0).setItems(requestResponse.getChunkItemsForRequest()).build();
        imsMessageProcessorBean.handleConsumedMessage(getConsumedMessageForChunk(chunk));
    }

    private final ImsMessageProcessorBean imsMessageProcessorBean = new ImsMessageProcessorBean();
    {
        imsMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        imsMessageProcessorBean.imsConfigBean = imsConfigBean;
        imsMessageProcessorBean.metricsHandler = metricsHandler;
    }

    private ConsumedMessage getConsumedMessageForChunk(Chunk chunk) {
        try {
            final Map<String, Object> headers = new HashMap<>();
            headers.put(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);

            return new ConsumedMessage("messageId", headers, jsonbContext.marshall(chunk));
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }

    private Chunk getIgnoredChunk() {
        return new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder().setStatus(ChunkItem.Status.IGNORE).build()))
                .build();
    }

    private String getWireMockEndpoint() {
        return String.format("http://localhost:%d/", wireMockRule.port());
    }
}
