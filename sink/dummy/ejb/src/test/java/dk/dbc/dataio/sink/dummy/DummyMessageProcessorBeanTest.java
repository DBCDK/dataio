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

package dk.dbc.dataio.sink.dummy;

import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.types.jms.JmsConstants;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnectorException;
import dk.dbc.dataio.commons.utils.jobstore.ejb.JobStoreServiceConnectorBean;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jsonb.JSONBContext;
import dk.dbc.dataio.jsonb.JSONBException;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class DummyMessageProcessorBeanTest {
    private final JobStoreServiceConnectorBean jobStoreServiceConnectorBean = mock(JobStoreServiceConnectorBean.class);
    private JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private Map<String, Object> headers = Collections.singletonMap(JmsConstants.PAYLOAD_PROPERTY_NAME, JmsConstants.CHUNK_PAYLOAD_TYPE);

    @Before
    public void setupMocks() {
        when(jobStoreServiceConnectorBean.getConnector()).thenReturn(jobStoreServiceConnector);
    }

    @Test
    public void handleConsumedMessage_onValidInputMessage_newOutputMessageEnqueued() throws ServiceException, InvalidMessageException, JSONBException, JobStoreServiceConnectorException {
        final String messageId = "id";
        final Chunk processedChunk = new ChunkBuilder(Chunk.Type.PROCESSED).setJobId(0L).setChunkId(0L).build();
        final String payload = new JSONBContext().marshall(processedChunk);
        final ConsumedMessage consumedMessage = new ConsumedMessage(messageId, headers, payload);
        getDummyMessageProcessorBean().handleConsumedMessage(consumedMessage);

        verify(jobStoreServiceConnector).addChunkIgnoreDuplicates(any(Chunk.class), anyLong(), anyLong());
    }

    @Test
    public void processPayload_chunkResultArgIsNonEmpty_returnsNonEmptyDeliveredChunk() {
        final List<ChunkItem> processedChunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build()
        );
        final Chunk chunkResult = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(processedChunkItems)
                .build();

        final Chunk deliveredChunk = getDummyMessageProcessorBean().processPayload(chunkResult);
        assertThat(deliveredChunk.size(), is(processedChunkItems.size()));
        Iterator<ChunkItem> iterator = deliveredChunk.iterator();
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item0 = iterator.next();
        assertThat(item0.getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item1 = iterator.next();
        assertThat(item1.getStatus(), is(processedChunkItems.get(1).getStatus()));
        assertThat(iterator.hasNext(), is(true));
        ChunkItem item2 = iterator.next();
        assertThat(item2.getStatus(), is(processedChunkItems.get(2).getStatus()));
        assertThat(iterator.hasNext(), is(false));
    }

    private DummyMessageProcessorBean getDummyMessageProcessorBean() {
        final DummyMessageProcessorBean dummyMessageProcessorBean = new DummyMessageProcessorBean();
        dummyMessageProcessorBean.jobStoreServiceConnectorBean = jobStoreServiceConnectorBean;
        return dummyMessageProcessorBean;
    }
}
