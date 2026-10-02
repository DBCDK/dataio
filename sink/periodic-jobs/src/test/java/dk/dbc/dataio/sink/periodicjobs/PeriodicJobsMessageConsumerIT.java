package dk.dbc.dataio.sink.periodicjobs;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import org.junit.Test;
import org.testcontainers.shaded.com.fasterxml.jackson.core.JsonProcessingException;
import org.testcontainers.shaded.com.fasterxml.jackson.databind.ObjectMapper;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class PeriodicJobsMessageConsumerIT extends IntegrationTest {
    private final AddiRecord addiRecord1 = newAddiRecord(new ConversionParam(), "record-1");
    private final AddiRecord addiRecord2 = newAddiRecord(new PeriodicJobsConversionParam().withSortkey("custom-sortkey").withRecordHeader("custom-header\n"), "record-2");

    /**
     * The datablock keys asserted here are what pins the record number against the item
     * ids the message headers carry, since they decide where each converted record lands
     * in the sort order of the file delivered when the job ends.
     */
    @Test
    public void convertItems() {
        PeriodicJobsMessageConsumer periodicJobsMessageConsumer = newMessageConsumerBean();

        List<ChunkItem> chunkItems = Arrays.asList(
                new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(),
                new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).setData("non-addi").build(),
                new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build(),
                new ChunkItemBuilder().setId(3L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build(),
                new ChunkItemBuilder().setId(4L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord2.getBytes()).build());
        final int jobId = 42;
        final int chunkId = 0;

        List<ChunkItem> outcomes = env().getPersistenceContext().run(() -> {
            List<ChunkItem> results = new ArrayList<>();
            for (ChunkItem chunkItem : chunkItems) {
                results.add(periodicJobsMessageConsumer.convertItem(chunkItem, jobId, chunkId,
                        (short) chunkItem.getId(), env().getEntityManager()));
            }
            return results;
        });

        assertThat("number of outcomes", outcomes.size(), is(5));
        assertThat("1st outcome", outcomes.get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd outcome", outcomes.get(1).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("3rd outcome", outcomes.get(2).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("4th outcome", outcomes.get(3).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th outcome", outcomes.get(4).getStatus(), is(ChunkItem.Status.SUCCESS));

        PeriodicJobsDataBlock.Key key1 = new PeriodicJobsDataBlock.Key(jobId, 1, 0);
        PeriodicJobsDataBlock datablock1 = env().getPersistenceContext().run(() -> env().getEntityManager().find(PeriodicJobsDataBlock.class, key1));
        PeriodicJobsDataBlock expectedDatablock1 = new PeriodicJobsDataBlock();
        expectedDatablock1.setKey(key1);
        expectedDatablock1.setSortkey("000000001");
        expectedDatablock1.setBytes("non-addi".getBytes(StandardCharsets.UTF_8));
        assertThat("1st datablock written", datablock1, is(expectedDatablock1));

        PeriodicJobsDataBlock.Key key2 = new PeriodicJobsDataBlock.Key(jobId, 3, 0);
        PeriodicJobsDataBlock datablock2 = env().getPersistenceContext().run(() -> env().getEntityManager().find(PeriodicJobsDataBlock.class, key2));
        PeriodicJobsDataBlock expectedDatablock2 = new PeriodicJobsDataBlock();
        expectedDatablock2.setKey(key2);
        expectedDatablock2.setSortkey("000000003");
        expectedDatablock2.setBytes("record-1".getBytes(StandardCharsets.UTF_8));
        assertThat("2nd datablock written", datablock2, is(expectedDatablock2));

        PeriodicJobsDataBlock.Key key3 = new PeriodicJobsDataBlock.Key(jobId, 4, 0);
        PeriodicJobsDataBlock datablock3 = env().getPersistenceContext().run(() -> env().getEntityManager().find(PeriodicJobsDataBlock.class, key3));
        PeriodicJobsDataBlock expectedDatablock3 = new PeriodicJobsDataBlock();
        expectedDatablock3.setKey(key3);
        expectedDatablock3.setSortkey("custom-sortkey");
        expectedDatablock3.setBytes("custom-header\nrecord-2".getBytes(StandardCharsets.UTF_8));
        assertThat("3rd datablock written", datablock3, is(expectedDatablock3));
    }

    @Test
    public void overwriteExistingDataBlock() {
        final int jobId = 42;
        final int chunkId = 7;
        ChunkItem chunkItem = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS)
                .setData(addiRecord1.getBytes()).build();

        PeriodicJobsDataBlock.Key key = new PeriodicJobsDataBlock.Key(jobId, 70, 0);
        PeriodicJobsDataBlock existingDatablock = new PeriodicJobsDataBlock();
        existingDatablock.setKey(key);
        existingDatablock.setSortkey("000000070");
        existingDatablock.setBytes("record-70".getBytes(StandardCharsets.UTF_8));

        env().getPersistenceContext().run(() -> {
            env().getEntityManager().persist(existingDatablock);
        });

        PeriodicJobsMessageConsumer periodicJobsMessageConsumer = newMessageConsumerBean();

        ChunkItem outcome = env().getPersistenceContext().run(() ->
                periodicJobsMessageConsumer.convertItem(chunkItem, jobId, chunkId, (short) 0,
                        env().getEntityManager()));

        assertThat("outcome", outcome.getStatus(), is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void emptyConversionResultsFails() {
        final int jobId = 42;
        final int chunkId = 0;
        PeriodicJobsMessageConsumer periodicJobsMessageConsumer = newMessageConsumerBean();
        ChunkItem chunkItem = new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS)
                .setData(newAddiRecord(new ConversionParam(), "").getBytes()).build();

        ChunkItem outcome = env().getPersistenceContext().run(() ->
                periodicJobsMessageConsumer.convertItem(chunkItem, jobId, chunkId, (short) 0,
                        env().getEntityManager()));
        assertThat("outcome", outcome.getStatus(), is(ChunkItem.Status.FAILURE));

        PeriodicJobsDataBlock.Key key = new PeriodicJobsDataBlock.Key(jobId, 0, 0);
        PeriodicJobsDataBlock datablock = env().getPersistenceContext().run(() -> env().getEntityManager().find(PeriodicJobsDataBlock.class, key));
        assertThat("datablock not written", datablock, is(nullValue()));
    }

    private PeriodicJobsMessageConsumer newMessageConsumerBean() {
        PeriodicJobsMessageConsumer periodicJobsMessageConsumer = new PeriodicJobsMessageConsumer(new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build(), env().getEntityManagerFactory());
        return periodicJobsMessageConsumer;
    }

    private AddiRecord newAddiRecord(ConversionParam conversionParam, String data) {
        try {
            byte[] metadata = new ObjectMapper().writeValueAsBytes(conversionParam);
            byte[] record = data.getBytes(StandardCharsets.UTF_8);
            return new AddiRecord(metadata, record);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
}
