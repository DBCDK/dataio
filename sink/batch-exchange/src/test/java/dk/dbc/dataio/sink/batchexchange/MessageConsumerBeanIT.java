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

package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.batchexchange.dto.Batch;
import dk.dbc.batchexchange.dto.BatchEntry;
import dk.dbc.batchexchange.dto.Diagnostic;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.types.ConsumedMessage;
import dk.dbc.dataio.commons.types.Priority;
import dk.dbc.dataio.commons.types.exceptions.InvalidMessageException;
import dk.dbc.dataio.commons.types.exceptions.ServiceException;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.sink.testutil.ObjectFactory;
import org.eclipse.microprofile.metrics.MetricRegistry;
import org.junit.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.mock;

public class MessageConsumerBeanIT extends IntegrationTest {
    private final String addiMetadata = "<referenceData><info submitter=\"424242\"/></referenceData>";

    private final AddiRecord addiRecordWithInvalidMetadata = new AddiRecord(
            "Invalid XML metadata".getBytes(), "content".getBytes());

    final private MetricRegistry metricRegistry = mock(MetricRegistry.class);

    /* Given: a consumed message containing a chunk where the first item is failed by processor
     *   And: the second item is ignored by processor
     *   And: the third item contains an invalid addi record
     *   And: the fourth item contains a single valid addi record
     *   And: the fifth item contains three valid addi record
     *  When: the message is handled by the consumer
     *  Then: a batch with seven entries is created in the batch exchange
     */
    @Test
    public void handleConsumedMessage() throws ServiceException, InvalidMessageException, IOException {
        // Given...

        final AddiRecord addiRecordX = new AddiRecord(
                addiMetadata.getBytes(), "contentX".getBytes());
        final AddiRecord addiRecordY = new AddiRecord(
                addiMetadata.getBytes(), "contentY".getBytes());

        final List<ChunkItem> chunkItems = new ArrayList<>();

        // failed item
        chunkItems.add(new ChunkItemBuilder()
                .setId(0)
                .setStatus(ChunkItem.Status.FAILURE)
                .setTrackingId("zero")
                .build());

        // ignored item
        chunkItems.add(new ChunkItemBuilder()
                .setId(1)
                .setStatus(ChunkItem.Status.IGNORE)
                .setTrackingId("one")
                .build());

        // invalid addi
        chunkItems.add(new ChunkItemBuilder()
                .setId(2)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(addiRecordWithInvalidMetadata.getBytes())
                .setTrackingId("two")
                .build());

        // item with single addi record
        chunkItems.add(new ChunkItemBuilder()
                .setId(3)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(addiRecordX.getBytes())
                .setTrackingId(null)
                .build());

        // item with three addi records
        final ByteArrayOutputStream outputStream = new ByteArrayOutputStream( );
        outputStream.write(addiRecordX.getBytes());
        outputStream.write(addiRecordY.getBytes());
        outputStream.write(addiRecordX.getBytes());
        chunkItems.add(new ChunkItemBuilder()
                .setId(4)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(outputStream.toByteArray())
                .setTrackingId("four")
                .build());

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(chunkItems)
                .build();

        // When...

        persistenceContext.run(() -> {
            final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk, Priority.HIGH);
            final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();
            messageConsumerBean.handleConsumedMessage(message);
        });

        // Then...

        final Batch batch = entityManager.find(Batch.class, 1);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch status", batch.getStatus(), is(Batch.Status.PENDING));
        assertThat("batch name", batch.getName(), is(BatchName.fromChunk(chunk).toString()));

        @SuppressWarnings("unchecked")
        final List<BatchEntry> entries = (List<BatchEntry>) entityManager
                .createNamedQuery(BatchEntry.GET_BATCH_ENTRIES_QUERY_NAME)
                .setParameter(1, batch.getId())
                .setHint("eclipselink.refresh", true)
                .getResultList();

        assertThat("number of batch entries", entries.size(), is(7));

        BatchEntry entry = entries.get(0);
        assertThat("1st entry status", entry.getStatus(), is(BatchEntry.Status.IGNORED));
        assertThat("1st entry diagnostics", entry.getDiagnostics().size(), is(1));
        assertThat("1st entry diagnostic level", entry.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.OK));
        assertThat("1st entry diagnostic message", entry.getDiagnostics().get(0).getMessage(), is("Failed by processor"));
        assertThat("1st entry tracking ID", entry.getTrackingId(), is(chunk.getItems().get(0).getTrackingId()));
        assertThat("1st entry priority", entry.getPriority(), is(Priority.HIGH.getValue()));

        entry = entries.get(1);
        assertThat("2nd entry status", entry.getStatus(), is(BatchEntry.Status.IGNORED));
        assertThat("2nd entry diagnostics", entry.getDiagnostics().size(), is(1));
        assertThat("2nd entry diagnostic level", entry.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.OK));
        assertThat("2nd entry diagnostic message", entry.getDiagnostics().get(0).getMessage(), is("Ignored by processor"));
        assertThat("2nd entry tracking ID", entry.getTrackingId(), is(chunk.getItems().get(1).getTrackingId()));
        assertThat("2nd entry priority", entry.getPriority(), is(Priority.HIGH.getValue()));

        entry = entries.get(2);
        assertThat("3rd entry status", entry.getStatus(), is(BatchEntry.Status.FAILED));
        assertThat("3rd entry diagnostics", entry.getDiagnostics().size(), is(1));
        assertThat("3rd entry diagnostic level", entry.getDiagnostics().get(0).getLevel(), is(Diagnostic.Level.ERROR));
        assertThat("3rd entry tracking ID", entry.getTrackingId(), is(chunk.getItems().get(2).getTrackingId()));
        assertThat("3rd entry priority", entry.getPriority(), is(Priority.HIGH.getValue()));

        entry = entries.get(3);
        assertThat("4th entry status", entry.getStatus(), is(BatchEntry.Status.PENDING));
        assertThat("4th entry content", entry.getContent(), is(addiRecordX.getContentData()));
        assertThat("4th entry metadata", entry.getMetadata(), is("{\"submitter\": \"424242\"}"));
        assertThat("4th entry is continued", entry.getContinued(), is(false));
        assertThat("4th entry tracking ID", entry.getTrackingId(),
                is(String.format("io:%s-%d", batch.getName(), chunk.getItems().get(3).getId())));
        assertThat("4th entry priority", entry.getPriority(), is(Priority.HIGH.getValue()));

        entry = entries.get(4);
        assertThat("5th entry status", entry.getStatus(), is(BatchEntry.Status.PENDING));
        assertThat("5th entry content", entry.getContent(), is(addiRecordX.getContentData()));
        assertThat("5th entry metadata", entry.getMetadata(), is("{\"submitter\": \"424242\"}"));
        assertThat("5th entry is continued", entry.getContinued(), is(true));
        assertThat("5th entry tracking ID", entry.getTrackingId(), is(chunk.getItems().get(4).getTrackingId()));
        assertThat("5th entry priority", entry.getPriority(), is(Priority.HIGH.getValue()));

        entry = entries.get(5);
        assertThat("6th entry status", entry.getStatus(), is(BatchEntry.Status.PENDING));
        assertThat("6th entry content", entry.getContent(), is(addiRecordY.getContentData()));
        assertThat("6th entry metadata", entry.getMetadata(), is("{\"submitter\": \"424242\"}"));
        assertThat("6th entry is continued", entry.getContinued(), is(true));
        assertThat("6th entry tracking ID", entry.getTrackingId(), is(chunk.getItems().get(4).getTrackingId()));
        assertThat("6th entry priority", entry.getPriority(), is(Priority.HIGH.getValue()));

        entry = entries.get(6);
        assertThat("7th entry status", entry.getStatus(), is(BatchEntry.Status.PENDING));
        assertThat("7th entry content", entry.getContent(), is(addiRecordX.getContentData()));
        assertThat("7th entry metadata", entry.getMetadata(), is("{\"submitter\": \"424242\"}"));
        assertThat("7th entry is continued", entry.getContinued(), is(false));
        assertThat("7th entry tracking ID", entry.getTrackingId(), is(chunk.getItems().get(4).getTrackingId()));
        assertThat("7th entry priority", entry.getPriority(), is(Priority.HIGH.getValue()));
    }

    /* Given: a consumed message containing a chunk where the first item is failed by processor
     *   And: the second item is ignored by processor
     *   And: the third item contains an invalid addi record
     *  When: the message is handled by the consumer
     *  Then: a batch with status completed is created since it contains no pending entries
     */
    @Test
    public void handleConsumedMessage_completesBatchWhenNoIncompleteEntriesExist() {
        // Given...

        final List<ChunkItem> chunkItems = new ArrayList<>();

        // failed item
        chunkItems.add(new ChunkItemBuilder()
                .setId(0)
                .setStatus(ChunkItem.Status.FAILURE)
                .setTrackingId("zero")
                .build());

        // ignored item
        chunkItems.add(new ChunkItemBuilder()
                .setId(1)
                .setStatus(ChunkItem.Status.IGNORE)
                .setTrackingId("one")
                .build());

        // invalid addi
        chunkItems.add(new ChunkItemBuilder()
                .setId(2)
                .setStatus(ChunkItem.Status.SUCCESS)
                .setData(addiRecordWithInvalidMetadata.getBytes())
                .setTrackingId("two")
                .build());

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setItems(chunkItems)
                .build();

        // When...

        persistenceContext.run(() -> {
            final ConsumedMessage message = ObjectFactory.createConsumedMessage(chunk);
            final MessageConsumerBean messageConsumerBean = createMessageConsumerBean();
            messageConsumerBean.handleConsumedMessage(message);
        });

        // Then...

        final Batch batch = entityManager.find(Batch.class, 1);
        assertThat("batch created", batch, is(notNullValue()));
        assertThat("batch status", batch.getStatus(), is(Batch.Status.COMPLETED));
        assertThat("batch name", batch.getName(), is(BatchName.fromChunk(chunk).toString()));
    }

    private MessageConsumerBean createMessageConsumerBean() {
        final MessageConsumerBean bean = new MessageConsumerBean();
        bean.entityManager = entityManager;
        bean.metricRegistry = metricRegistry;
        return bean;
    }
}
