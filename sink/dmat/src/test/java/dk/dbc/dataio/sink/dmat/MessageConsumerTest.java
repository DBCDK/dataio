package dk.dbc.dataio.sink.dmat;

import dk.dbc.commons.addi.AddiReader;
import dk.dbc.commons.addi.AddiRecord;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.ChunkItem;
import dk.dbc.dataio.commons.utils.jobstore.JobStoreServiceConnector;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.jse.artemis.common.service.ServiceHub;
import dk.dbc.dmat.service.connector.DMatServiceConnector;
import dk.dbc.dmat.service.connector.DMatServiceConnectorException;
import dk.dbc.dmat.service.dto.RecordData;
import dk.dbc.dmat.service.persistence.DMatRecord;
import dk.dbc.dmat.service.persistence.enums.EReolCode;
import dk.dbc.dmat.service.persistence.enums.Status;
import org.hamcrest.MatcherAssert;
import org.hamcrest.core.Is;
import org.junit.Before;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class MessageConsumerTest {
    private final DMatServiceConnector dMatServiceConnector = mock(DMatServiceConnector.class);
    private final JobStoreServiceConnector jobStoreServiceConnector = mock(JobStoreServiceConnector.class);
    private final MessageConsumer messageConsumer = new MessageConsumer(
            new ServiceHub.Builder().withJobStoreServiceConnector(jobStoreServiceConnector).build(),
            dMatServiceConnector);

    @Before
    public void setupMocks() throws DMatServiceConnectorException, JSONBException {
        when(dMatServiceConnector.upsertRecord(any(RecordData.class))).thenReturn(new DMatRecord().withId(1).withStatus(Status.NEW));
    }

    private byte[] readLocalFile(String name) throws IOException {
        Path path = Paths.get(MessageConsumerTest.class.getResource("/__files/" + name).getPath());
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

        final Chunk result = messageConsumer.handleChunk(chunk);

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

        final Chunk result = messageConsumer.handleChunk(chunk);

        MatcherAssert.assertThat("number of chunk items", result.size(), is(2));
        MatcherAssert.assertThat("1st chunk item", result.getItems().get(0).getStatus(),
                is(ChunkItem.Status.FAILURE));
        MatcherAssert.assertThat("2nd chunk item", result.getItems().get(1).getStatus(),
                is(ChunkItem.Status.FAILURE));
    }

    /**
     * This test mimics a test in the dmat-service, to check that the RecordData object correctly
     * serializes and deserializes the incomming addi data, thus checking that we have the proper
     * versions of the dmat-connector (and by transitive dependency) the dmat-model
     *
     * @throws IOException
     * @throws JSONBException
     */
    @Test
    public void testRecordData() throws IOException, JSONBException {
        byte[] validRecordAddi = readLocalFile("valid_recorddata_addi.json");
        InputStream is = new ByteArrayInputStream(validRecordAddi);
        AddiReader rdr = new AddiReader(is);
        AddiRecord addiRecord = rdr.getNextRecord();

        RecordData record = RecordData.fromRaw(new String(addiRecord.getContentData()));

        assertThat("datestamp", record.getDatestamp(), Is.is("20150706T12:09:19Z"));
        assertThat("recordReference", record.getRecordReference(), Is.is("33707b62-99d2-47a0-ad51-a7a35e53493c"));
        assertThat("active", record.getActive(), Is.is(true));
        assertThat("isbn13", record.getIsbn13(), Is.is("9788772196244"));
        assertThat("productForm", record.getProductForm(), Is.is("DIGITAL_DOWNLOAD"));
        assertThat("contentType", record.getContentType(), Is.is("TEXT_EYE_READABLE"));
        assertThat("title.text", record.getTitle().getText(), Is.is("Leadership pipeline"));
        assertThat("publisherName", record.getPublisherName(), Is.is("Gyldendal"));
        assertThat("publishingDate", record.getPublishingDate(), Is.is("20150202"));
        assertThat("lendingTypes", record.getLendingTypes(), Is.is(notNullValue()));
        assertThat("lendingTypes", record.getLendingTypes().size(), Is.is(1));
        assertThat("lendingTypes contains ERL", record.getLendingTypes().get(0), Is.is(EReolCode.ERE));
    }
}
