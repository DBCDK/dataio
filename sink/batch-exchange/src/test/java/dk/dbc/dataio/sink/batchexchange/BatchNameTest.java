package dk.dbc.dataio.sink.batchexchange;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import org.junit.Assert;
import org.junit.jupiter.api.Test;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

public class BatchNameTest {
    @Test
    public void constructor() {
        final int jobId = 4242;
        final long chunkId = 2424;
        final BatchName batchName = new BatchName(jobId, chunkId);
        assertThat("jobId", batchName.getJobId(), is(jobId));
        assertThat("chunkId", batchName.getChunkId(), is(chunkId));
    }

    @Test
    public void fromChunk() {
        final Chunk chunk = new ChunkBuilder(Chunk.Type.PROCESSED).build();
        final BatchName batchName = BatchName.fromChunk(chunk);
        assertThat("jobId", batchName.getJobId(), is(chunk.getJobId()));
        assertThat("chunkId", batchName.getChunkId(), is(chunk.getChunkId()));
    }

    @Test
    public void fromString_invalidNumberOfTokens_throws() {
        Assert.assertThrows(IllegalArgumentException.class, () -> BatchName.fromString("1-2-3"));
    }

    @Test
    public void fromString_invalidTokenTypes_throws() {
        Assert.assertThrows(IllegalArgumentException.class, () -> BatchName.fromString("one-two"));
    }

    @Test
    public void fromString() {
        final int jobId = 4242;
        final long chunkId = 2424;
        final BatchName batchName = BatchName.fromString(String.format("%d-%d", jobId, chunkId));
        assertThat("jobId", batchName.getJobId(), is(jobId));
        assertThat("chunkId", batchName.getChunkId(), is(chunkId));
    }
}
