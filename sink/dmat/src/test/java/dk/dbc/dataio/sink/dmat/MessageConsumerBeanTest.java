package dk.dbc.dataio.sink.dmat;

import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.commons.metricshandler.CounterMetric;
import dk.dbc.commons.metricshandler.MetricsHandlerBean;
import dk.dbc.commons.metricshandler.SimpleTimerMetric;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.RecordData;
import dk.dbc.dmat.service.persistence.enums.Status;
import dk.dbc.dmat.service.persistence.DMatRecord;
import org.hamcrest.MatcherAssert;
import org.junit.Before;
import org.junit.Test;
import org.junit.jupiter.api.BeforeAll;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageConsumerBeanTest {
    private static final Logger LOGGER = LoggerFactory.getLogger(MessageConsumerBeanTest.class);
    private MessageConsumerBean messageConsumerBean = new MessageConsumerBean();

    @Before
    public void setupMocks() throws DMatServiceConnectorException, JSONBException {
        messageConsumerBean.connector = mock(DMatServiceConnector.class);
        when(messageConsumerBean.connector.upsertRecord(any(RecordData.class))).thenReturn(
                        new DMatRecord().withId(1).withStatus(Status.NEW));

        messageConsumerBean.metricsHandler = mock(MetricsHandlerBean.class);
        doNothing().when(messageConsumerBean.metricsHandler).increment(any(CounterMetric.class));
        doNothing().when(messageConsumerBean.metricsHandler).update(any(SimpleTimerMetric.class), any());
    }

    private byte[] readLocalFile(String name) throws IOException {
        Path path = Paths.get(MessageConsumerBeanTest.class.getResource("/__files/" + name).getPath());
        return Files.readAllBytes(path);
    }

    @Test
    public void testHandleChunk() throws IOException {

        byte[] validRecordAddi = readLocalFile("valid_recorddata_addi.json");

        final List<ChunkItem> chunkItems = Arrays.asList(
                ChunkItem.failedChunkItem().withId(0L),     // failed by job processor
                ChunkItem.ignoredChunkItem().withId(1L),    // ignored by job processor
                ChunkItem.successfulChunkItem().withId(2L)  // invalid input record (not json)
                        .withData("invalid-chunk"),
                ChunkItem.successfulChunkItem().withId(3L)  // successfully delivered
                        .withData(validRecordAddi)
        );

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = messageConsumerBean.handleChunk(chunk);

        MatcherAssert.assertThat("number of chunk items", result.size(), is(4));
        MatcherAssert.assertThat("1st chunk item", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.IGNORE));
        MatcherAssert.assertThat("2nd chunk item", result.getItems().get(1).getStatus(),
                is(ChunkItem.Status.IGNORE));
        MatcherAssert.assertThat("3rd chunk item", result.getItems().get(2).getStatus(),
                is(ChunkItem.Status.FAILURE));
        MatcherAssert.assertThat("4th chunk item", result.getItems().get(3).getStatus(),
                is(ChunkItem.Status.SUCCESS));
    }

    @Test
    public void testHandleChunkWithError() throws IOException {

        byte[] invalidRecordAddiNoDatestamp = readLocalFile("invalid_recorddata_addi_no_datestamp.json");
        byte[] invalidRecordAddiNoRecordReference = readLocalFile("invalid_recorddata_addi_no_recordreference.json");

        final List<ChunkItem> chunkItems = Arrays.asList(
                ChunkItem.successfulChunkItem().withId(0L)  // failed du to invalid record (missing datestamp)
                        .withData(invalidRecordAddiNoDatestamp),
                ChunkItem.successfulChunkItem().withId(1L)  // failed du to invalid record (missing record reference)
                        .withData(invalidRecordAddiNoRecordReference)
        );

        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED)
                .setChunkId(0L)
                .setItems(chunkItems)
                .build();

        final Chunk result = messageConsumerBean.handleChunk(chunk);

        MatcherAssert.assertThat("number of chunk items", result.size(), is(2));
        MatcherAssert.assertThat("1st chunk item", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.FAILURE));
        MatcherAssert.assertThat("2nd chunk item", result.getItems().get(1).getStatus(),
                is(ChunkItem.Status.FAILURE));
    }
}
