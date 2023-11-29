package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class BatchNameTest {
    @Test
    public void constructor() {
        final int jobId = 4242;
        final long chunkId = 2424;
        BatchName batchName = new BatchName(jobId, chunkId);
        assertThat("jobId", batchName.getJobId(), is(jobId));
        assertThat("chunkId", batchName.getChunkId(), is(chunkId));
    }

    @Test
    public void fromChunk() {
        Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        BatchName batchName = BatchName.fromChunk(chunk);
        assertThat("jobId", batchName.getJobId(), is(chunk.getJobId()));
        assertThat("chunkId", batchName.getChunkId(), is(chunk.getChunkId()));
    }

    @Test
    public void fromString_invalidNumberOfTokens_throws() {
        assertThrows(IllegalArgumentException.class, () -> BatchName.fromString("1-2-3"));
    }

    @Test
    public void fromString_invalidTokenTypes_throws() {
        assertThrows(IllegalArgumentException.class, () -> BatchName.fromString("one-two"));
    }

    @Test
    public void fromString() {
        final int jobId = 4242;
        final long chunkId = 2424;
        BatchName batchName = BatchName.fromString(String.format("%d-%d", jobId, chunkId));
        assertThat("jobId", batchName.getJobId(), is(jobId));
        assertThat("chunkId", batchName.getChunkId(), is(chunkId));
    }
}
