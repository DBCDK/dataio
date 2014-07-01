package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Test;

public class NaiveSequenceAnalyserTest {

    @Test
    public void testInsertionAndRetrivalOfSingleChunk() {
        NaiveSequenceAnalyser sa = new NaiveSequenceAnalyser();
        Chunk chunk = new ChunkBuilder().setJobId(1L).setChunkId(2L).build();
        Sink sink = new SinkBuilder().build();
        // add chunk
        sa.addChunk(chunk, sink);
        // verify that chunk is independent
        List<ChunkIdentifier> independentChunks = sa.getInactiveIndependentChunks();
        assertThat(independentChunks.size(), is(1));
        assertThat(independentChunks.get(0).jobId, is(1L));
        assertThat(independentChunks.get(0).chunkId, is(2L));
        // remove chunk
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L,2L));
        // verify that there are no independent chunks left
        assertThat(sa.getInactiveIndependentChunks().size(), is(0));
    }

    @Test
    public void testInsertionAndRetrivalOfTwoIndependentChunks() {
        NaiveSequenceAnalyser sa = new NaiveSequenceAnalyser();
        Chunk chunk1 = new ChunkBuilder().setJobId(1L).setChunkId(2L).build();
        Chunk chunk2 = new ChunkBuilder().setJobId(3L).setChunkId(4L).build();
        Sink sink = new SinkBuilder().build();
        // add chunk
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        // verify that chunks are independent
        List<ChunkIdentifier> independentChunks = sa.getInactiveIndependentChunks();
        assertThat(independentChunks.size(), is(2));
        assertThat(independentChunks.get(0).jobId, is(1L));
        assertThat(independentChunks.get(0).chunkId, is(2L));
        assertThat(independentChunks.get(1).jobId, is(3L));
        assertThat(independentChunks.get(1).chunkId, is(4L));
        // remove chunk1
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L,2L));
        // verify that chunk2 is still present
        independentChunks = sa.getInactiveIndependentChunks();
        assertThat(independentChunks.size(), is(1));
        assertThat(independentChunks.get(0).jobId, is(3L));
        assertThat(independentChunks.get(0).chunkId, is(4L));
        // remove chunk2
        sa.deleteAndReleaseChunk(new ChunkIdentifier(3L,4L));
        // verify that there are no independent chunks left
        assertThat(sa.getInactiveIndependentChunks().size(), is(0));
    }
}
