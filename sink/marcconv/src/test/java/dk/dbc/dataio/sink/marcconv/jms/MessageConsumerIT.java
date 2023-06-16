package dk.dbc.dataio.sink.marcconv.jms;

import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.dataio.commons.conversion.ConversionParam;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.lang.ResourceReader;
import dk.dbc.dataio.commons.utils.lang.StringUtil;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.ChunkItemBuilder;
import dk.dbc.dataio.filestore.service.connector.FileStoreServiceConnector;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dataio.sink.marcconv.IntegrationTest;
import dk.dbc.dataio.sink.marcconv.entity.ConversionBlock;
import dk.dbc.dataio.sink.marcconv.entity.StoredConversionParam;
import dk.dbc.jsonb.JSONBContext;
import dk.dbc.jsonb.JSONBException;
import org.junit.Test;
import org.mockito.Mockito;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.CoreMatchers.*;
import static org.hamcrest.MatcherAssert.assertThat;

public class MessageConsumerIT extends IntegrationTest {
    private final JSONBContext jsonbContext = new JSONBContext();
    private final ConversionParam conversionParam = new ConversionParam().withPackaging("iso").withEncoding("danmarc2");
    private final AddiRecord addiRecord1 = newAddiRecord(conversionParam, "test-record-1-danmarc2.marcxchange");
    private final byte[] isoRecord1 = ResourceReader.getResourceAsByteArray(MessageConsumerIT.class, "test-record-1-danmarc2.iso");
    private final AddiRecord addiRecord2 = newAddiRecord(conversionParam, "test-record-2-danmarc2.marcxchange");
    private final byte[] isoRecord2 = ResourceReader.getResourceAsByteArray(MessageConsumerIT.class, "test-record-2-danmarc2.iso");

    @Test
    public void handleChunk() {
        MessageConsumer messageConsumer = newMessageConsumerBean();

        List<ChunkItem> chunkItems = Arrays.asList(new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.FAILURE).build(), new ChunkItemBuilder().setId(1L).setStatus(ChunkItem.Status.SUCCESS).build(), new ChunkItemBuilder().setId(2L).setStatus(ChunkItem.Status.IGNORE).build(), new ChunkItemBuilder().setId(3L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build(), new ChunkItemBuilder().setId(4L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord2.getBytes()).build());
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setChunkId(0L).setItems(chunkItems).build();

        Chunk result = env().getPersistenceContext().run(() -> messageConsumer.handleChunk(chunk));
        assertThat("number of chunk items", result.size(), is(5));
        assertThat("1st chunk item", result.getItems().get(0).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("2nd chunk item", result.getItems().get(1).getStatus(), is(ChunkItem.Status.FAILURE));
        assertThat("3rd chunk item", result.getItems().get(2).getStatus(), is(ChunkItem.Status.IGNORE));
        assertThat("4th chunk item", result.getItems().get(3).getStatus(), is(ChunkItem.Status.SUCCESS));
        assertThat("5th chunk item", result.getItems().get(4).getStatus(), is(ChunkItem.Status.SUCCESS));

        assertThat("2nd chunk item diagnostics", result.getItems().get(1).getDiagnostics(), is(notNullValue()));

        ConversionBlock block = env().getPersistenceContext().run(() -> env().getEntityManager().find(ConversionBlock.class, new ConversionBlock.Key(chunk.getJobId(), chunk.getChunkId())));

        assertThat("block written", block, is(notNullValue()));

        byte[] expectedBytes = new byte[isoRecord1.length + isoRecord2.length];
        System.arraycopy(isoRecord1, 0, expectedBytes, 0, isoRecord1.length);
        System.arraycopy(isoRecord2, 0, expectedBytes, isoRecord1.length, isoRecord2.length);
        assertThat("block bytes", block.getBytes(), is(expectedBytes));

        StoredConversionParam storedConversionParam = env().getPersistenceContext().run(() -> env().getEntityManager().find(StoredConversionParam.class, Math.toIntExact(chunk.getJobId())));

        assertThat("StoredConversionParam", storedConversionParam, is(notNullValue()));
        assertThat("ConversionParam", storedConversionParam.getParam(), is(conversionParam));
    }

    @Test
    public void overwriteExistingBlock() {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setChunkId(7L).setItems(Collections.singletonList(new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord1.getBytes()).build())).build();

        ConversionBlock existingBlock = new ConversionBlock();
        existingBlock.setKey(new ConversionBlock.Key(chunk.getJobId(), chunk.getChunkId()));
        existingBlock.setBytes(StringUtil.asBytes("0"));

        env().getPersistenceContext().run(() -> env().getEntityManager().persist(existingBlock));

        MessageConsumer messageConsumer = newMessageConsumerBean();

        env().getPersistenceContext().run(() -> messageConsumer.handleChunk(chunk));

        ConversionBlock updatedBlock = env().getPersistenceContext().run(() -> env().getEntityManager().find(ConversionBlock.class, existingBlock.getKey()));

        assertThat("block bytes", updatedBlock.getBytes(), is(isoRecord1));
    }

    @Test
    public void dontStoreZeroLengthBlocks() {
        // The ISO2709 conversion can't handle the marcxchange slim format
        // and therefore fails the items.
        //
        // No Blocks should be persisted.

        ConversionParam conversionParam = new ConversionParam().withPackaging("iso").withEncoding("utf8");
        AddiRecord addiRecord = newAddiRecord(conversionParam, "test-record-3-marc21.slim");
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).setChunkId(0L).setItems(Collections.singletonList(new ChunkItemBuilder().setId(0L).setStatus(ChunkItem.Status.SUCCESS).setData(addiRecord.getBytes()).build())).build();

        MessageConsumer messageConsumer = newMessageConsumerBean();
        env().getPersistenceContext().run(() -> messageConsumer.handleChunk(chunk));

        ConversionBlock block = env().getPersistenceContext().run(() -> env().getEntityManager().find(ConversionBlock.class, new ConversionBlock.Key(chunk.getJobId(), chunk.getChunkId())));

        assertThat("block written", block, is(nullValue()));
    }

    private MessageConsumer newMessageConsumerBean() {
        ServiceHub hub = new ServiceHub.Builder().withJobStoreServiceConnector(Mockito.mock(JobStoreServiceConnector.class)).build();
        return new MessageConsumer(hub, Mockito.mock(FileStoreServiceConnector.class), env().getEntityManager());
    }

    private AddiRecord newAddiRecord(ConversionParam conversionParam, String resourceFile) {
        try {
            byte[] metadata = StringUtil.asBytes(jsonbContext.marshall(conversionParam));
            byte[] record = ResourceReader.getResourceAsByteArray(MessageConsumerIT.class, resourceFile);
            return new AddiRecord(metadata, record);
        } catch (JSONBException e) {
            throw new IllegalStateException(e);
        }
    }
}
