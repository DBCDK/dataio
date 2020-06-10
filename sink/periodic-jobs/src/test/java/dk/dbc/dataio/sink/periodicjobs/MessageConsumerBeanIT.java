/*
 * Copyright Dansk Bibliotekscenter a/s. Licensed under GNU GPLv3
 * See license text in LICENSE.txt
 */

package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageConsumerBeanIT extends IntegrationTest {
    private final AddiRecord addiRecord1 =
            newAddiRecord(new ConversionParam(), "record-1");
    private final AddiRecord addiRecord2 =
            newAddiRecord(new PeriodicJobsConversionParam()
                            .withSortkey("custom-sortkey")
                            .withRecordHeader("custom-header\n"),
                    "record-2");

    @Test
    public void handleChunk() {
        final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();

        final List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData("non-addi")
                        .build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecord1.getBytes())
                        .build(),
                new ChunkItemBuilder().setId(4L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(addiRecord2.getBytes())
                        .build()
        );
        final int jobId = 42;
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(jobId)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = env().getPersistenceContext().run(() ->
                messageConsumerBean.handleChunk(chunk));
        assertThat("number of chunk items", result.size(), is(5));
        assertThat("1st chunk item",
                result.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item",
                result.getItems().get(1).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("3rd chunk item",
                result.getItems().get(2).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("4th chunk item",
                result.getItems().get(3).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th chunk item",
                result.getItems().get(4).getStatus(), is(ChunkItem.Status.SUCCESS));

        final PeriodicJobsDataBlock.Key key1 = new PeriodicJobsDataBlock.Key(jobId, 1);
        final PeriodicJobsDataBlock datablock1 = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDataBlock.class, key1));
        final PeriodicJobsDataBlock expectedDatablock1 = new PeriodicJobsDataBlock();
        expectedDatablock1.setKey(key1);
        expectedDatablock1.setSortkey("000000001");
        expectedDatablock1.setBytes("non-addi".getBytes(StandardCharsets.UTF_8));
        assertThat("1st datablock written", datablock1, is(expectedDatablock1));

        final PeriodicJobsDataBlock.Key key2 = new PeriodicJobsDataBlock.Key(jobId, 3);
        final PeriodicJobsDataBlock datablock2 = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDataBlock.class, key2));
        final PeriodicJobsDataBlock expectedDatablock2 = new PeriodicJobsDataBlock();
        expectedDatablock2.setKey(key2);
        expectedDatablock2.setSortkey("000000003");
        expectedDatablock2.setBytes("record-1".getBytes(StandardCharsets.UTF_8));
        assertThat("2nd datablock written", datablock2, is(expectedDatablock2));

        final PeriodicJobsDataBlock.Key key3 = new PeriodicJobsDataBlock.Key(jobId, 4);
        final PeriodicJobsDataBlock datablock3 = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDataBlock.class, key3));
        final PeriodicJobsDataBlock expectedDatablock3 = new PeriodicJobsDataBlock();
        expectedDatablock3.setKey(key3);
        expectedDatablock3.setSortkey("custom-sortkey");
        expectedDatablock3.setBytes("custom-header\nrecord-2".getBytes(StandardCharsets.UTF_8));
        assertThat("3rd datablock written", datablock3, is(expectedDatablock3));
    }

    @Test
    public void overwriteExistingDataBlock() {
        final int jobId = 42;
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(jobId)
                .setChunkId(7L)
                .setItems(Collections.singletonList(
                        new ChunkItemBuilder()
                                .setId(0L)
                                .setStatus(ChunkItem.Status.SUCCESS)
                                .setData(addiRecord1.getBytes())
                                .build()
                ))
                .build();

        final PeriodicJobsDataBlock.Key key = new PeriodicJobsDataBlock.Key(jobId, 70);
        final PeriodicJobsDataBlock existingDatablock = new PeriodicJobsDataBlock();
        existingDatablock.setKey(key);
        existingDatablock.setSortkey("000000070");
        existingDatablock.setBytes("record-70".getBytes(StandardCharsets.UTF_8));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(existingDatablock);
        });

        final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();

        final Chunk result = env().getPersistenceContext().run(() ->
                messageConsumerBean.handleChunk(chunk));

        assertThat("1st chunk item",
                result.getItems().get(0).getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void emptyConversionResultsFails() {
        final int jobId = 42;
        final MessageConsumerBean messageConsumerBean = newMessageConsumerBean();
        final List<ChunkItem> chunkItems = Collections.singletonList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS)
                        .setData(newAddiRecord(new ConversionParam(), "").getBytes())
                        .build()
        );
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setJobId(jobId)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = env().getPersistenceContext().run(() ->
                messageConsumerBean.handleChunk(chunk));
        assertThat("1st chunk item",
                result.getItems().get(0).getStatus(), is(ChunkItem.Status.FAILURE));

        final PeriodicJobsDataBlock.Key key = new PeriodicJobsDataBlock.Key(jobId, 0);
        final PeriodicJobsDataBlock datablock = env().getPersistenceContext().run(() ->
                env().getEntityManager().find(PeriodicJobsDataBlock.class, key));
        assertThat("datablock not written", datablock, is(nullValue()));
    }

    private MessageConsumerBean newMessageConsumerBean() {
        final MessageConsumerBean messageConsumerBean = new MessageConsumerBean();
        messageConsumerBean.entityManager = env().getEntityManager();
        return messageConsumerBean;
    }

    private AddiRecord newAddiRecord(ConversionParam conversionParam, String data) {
        try {
            final byte[] metadata = StringUtil.asBytes(
                    new JSONBContext().marshall(conversionParam));
            final byte[] record = data.getBytes(StandardCharsets.UTF_8);
            return new AddiRecord(metadata, record);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}