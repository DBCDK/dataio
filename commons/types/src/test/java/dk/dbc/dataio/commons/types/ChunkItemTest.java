package dk.dbc.dataio.commons.types;


import dk.dbc.commons.jsonb.JSONBContext;
import dk.dbc.commons.jsonb.JSONBException;
import dk.dbc.dataio.commons.types.ChunkItem.Type;
import org.junit.Test;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static dk.dbc.commons.testutil.Assert.assertThat;
import static dk.dbc.commons.testutil.Assert.isThrowing;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

public class ChunkItemTest {
    private static final long ID = 1L;
    private static final byte[] DATA = "data".getBytes(StandardCharsets.UTF_8);
    private static final ChunkItem.Status STATUS = ChunkItem.Status.SUCCESS;

    @Test
    public void withData_dataArgIsNull_throws() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat(() -> chunkItem.withData((byte[]) null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withDiagnostics_diagnosticsArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
                .withDiagnostics((Diagnostic[]) null);
        assertThat(chunkItem.getDiagnostics(), is(nullValue()));
    }

    @Test
    public void withDiagnostics_diagnosticsCanBeAppended() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat("diagnostics before append", chunkItem.getDiagnostics(), is(nullValue()));

        chunkItem.withDiagnostics(ObjectFactory.buildFatalDiagnostic("Test Fatal"));
        assertThat("diagnostics after first append", chunkItem.getDiagnostics(), notNullValue());
        assertThat("number of diagnostics after first append", chunkItem.getDiagnostics().size(), is(1));
        assertThat("chunk item status", chunkItem.getStatus(), is(ChunkItem.Status.FAILURE));

        chunkItem.appendDiagnostics(ObjectFactory.buildFatalDiagnostic("Test Fatal2"));
        assertThat("diagnostics after second append", chunkItem.getDiagnostics(), notNullValue());
        assertThat("number of diagnostics after second append", chunkItem.getDiagnostics().size(), is(2));
    }

    @Test
    public void withEncoding_encodingArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
                .withEncoding(null);
        assertThat(chunkItem.getEncoding(), is(nullValue()));
    }

    @Test
    public void withId_idArgIsInvalid_throws() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat(() -> chunkItem.withId(Constants.CHUNK_ITEM_ID_LOWER_BOUND - 1), isThrowing(IllegalArgumentException.class));
    }

    @Test
    public void withStatus_statusArgIsNull_throws() {
        final ChunkItem chunkItem = new ChunkItem();
        assertThat(() -> chunkItem.withStatus(null), isThrowing(NullPointerException.class));
    }

    @Test
    public void withTrackingId_trackingIdArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
                .withTrackingId(null);
        assertThat(chunkItem.getTrackingId(), is(nullValue()));
    }

    @Test
    public void withTrackingId_trackingIdArgCanBeEmpty() {
        final ChunkItem chunkItem = new ChunkItem()
                .withTrackingId("");
        assertThat(chunkItem.getTrackingId(), is(""));
    }

    @Test
    public void withType_typeArgCanBeNull() {
        final ChunkItem chunkItem = new ChunkItem()
                .withType((Type[]) null);
        assertThat(chunkItem.getType(), is(nullValue()));
    }

    @Test
    public void constructor5arg_allArgsAreValid_returnsNewInstance() throws JSONBException {
        final ChunkItem instance = new ChunkItem(ID, DATA, STATUS, Arrays.asList(Type.UNKNOWN, Type.GENERICXML), StandardCharsets.UTF_8);
        assertThat(instance, is(notNullValue()));
        assertThat(instance.getId(), is(ID));
        assertThat(instance.getData(), is(DATA));
        assertThat(instance.getStatus(), is(STATUS));
        assertThat(instance.getType(), is(Arrays.asList(Type.UNKNOWN, Type.GENERICXML)));
        assertThat(instance.getEncoding(), is(StandardCharsets.UTF_8));
    }

    @Test
    public void unmarshallingChunkItem() throws JSONBException {
        final String json = "{\"id\":0,\"data\":\"MQ==\",\"status\":\"SUCCESS\"}";
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    @Test
    public void unmarshallingChunkItemWithTypeAndEncoding() throws JSONBException {
        final String json = "{\"id\":1,\"data\":\"ZGF0YQ==\",\"status\":\"SUCCESS\",\"type\":[\"STRING\",\"UNKNOWN\"],\"encoding\":\"UTF-8\"}";
        final JSONBContext jsonbContext = new JSONBContext();
        jsonbContext.unmarshall(json, ChunkItem.class);
    }

    public static ChunkItem newChunkItemInstance() {
        return new ChunkItem()
                .withId(ID)
                .withData(DATA)
                .withStatus(STATUS);
    }
}
