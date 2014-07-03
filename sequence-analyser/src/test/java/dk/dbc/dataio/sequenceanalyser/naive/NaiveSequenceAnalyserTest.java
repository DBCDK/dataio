package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

public class NaiveSequenceAnalyserTest {

    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

    XLogger LOGGER = XLoggerFactory.getXLogger(NaiveSequenceAnalyserTest.class);

    private SequenceAnalyser sa;

    @Before
    public void setup() {
        LOGGER.info("Testing");
        sa = new NaiveSequenceAnalyser();
    }

    @Test
    public void testInsertionAndRetrivalOfSingleChunk() {
        Chunk chunk = createChunk(1L, 2L);
        Sink sink = new SinkBuilder().build();
        // add chunk
        sa.addChunk(chunk, sink);
        // verify that chunk is independent
        assertChunks(sa.getInactiveIndependentChunks(), chunk);
        // remove chunk
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that there are no independent chunks left
        assertChunks(sa.getInactiveIndependentChunks());
        // verify that the sequenceanalyser is empty
        assertThat(sa.size(), is(0));
    }

    @Test
    public void testInsertionAndRetrivalOfTwoIndependentChunks() {
        Chunk chunk1 = createChunk(1L, 2L, "horse");
        Chunk chunk2 = createChunk(3L, 4L, "goat");
        Sink sink = new SinkBuilder().build();
        // add chunk
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        // verify that chunks are independent and inactive
        assertChunks(sa.getInactiveIndependentChunks(), chunk1, chunk2);
        // verify number of elements in Sequenceanalyser
        assertThat(sa.size(), is(2));
        // remove chunk1
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that a single chunk is still present in sequenceanalyser
        assertThat(sa.size(), is(1));
        // remove chunk2
        sa.deleteAndReleaseChunk(new ChunkIdentifier(3L, 4L));
        // verify that there are no independent inactive chunks left
        assertChunks(sa.getInactiveIndependentChunks());
        // verify that the sequenceanalyser is empty
        assertThat(sa.size(), is(0));
    }

    // Test: Two chunks where one depends on the other - when added only the first chunk can be independendent.
    //       But when first chunk is released, the second chunk must be independent.
    @Test
    public void test1() {
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        Sink sink = new SinkBuilder().build();
        // add chunks
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        // verify that chunks are dependent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
        // remove chunk1
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that chunk2 is now present
        assertChunks(sa.getInactiveIndependentChunks(), chunk2);
        // remove chunk2
        sa.deleteAndReleaseChunk(new ChunkIdentifier(3L, 4L));
        // verify that there are no independent chunks left
        assertChunks(sa.getInactiveIndependentChunks());
        // verify that the sequenceanalyser is empty
        assertThat(sa.size(), is(0));
    }

    // Test: Two chunks with overlapping keys, but destined for different sinks.
    //       They should not depend on each other.
    @Test
    public void test2() {
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        Sink sink1 = new SinkBuilder().setId(1L).build();
        Sink sink2 = new SinkBuilder().setId(2L).build();
        // add chunks
        sa.addChunk(chunk1, sink1);
        sa.addChunk(chunk2, sink2);
        // verify that chunks are independent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1, chunk2);
    }

    // Test: Three chunks where:
    //       - chunk1 is independent,
    //       - chunk2 depends on chunk1,
    //       - chunk3 depends on chunk1 and chunk2.
    //       - all three chunks are destined for the same sink.
    //       When chunk1 is released, chunk2 should be available but not chunk3, and
    //       when chunk2 is released, chunk3 should be available.
    @Test
    public void test3() {
        Chunk chunk1 = createChunk(1L, 2L, "animal", "bird", "eagle");
        Chunk chunk2 = createChunk(1L, 3L, "animal", "mammal", "goat");
        Chunk chunk3 = createChunk(4L, 5L, "animal", "mammal", "horse");
        Sink sink = new SinkBuilder().build();
        // add chunks
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        sa.addChunk(chunk3, sink);
        // verify that chunks are dependent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
        // remove chunk1
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that chunk2 is now present
        assertChunks(sa.getInactiveIndependentChunks(), chunk2);
        // remove chunk2
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 3L));
        // verify that chunk3 is now present
        assertChunks(sa.getInactiveIndependentChunks(), chunk3);
    }

    // Test:
    // When: a ChunkIdentifier is retrieved with getInactiveIndependentChunksAndActivate(),
    // Then: subsequent calls to getInactiveIndependentChunksAndActivate() will not return the same ChunkIdentifer,
    //       but the size of the internal dependecy graph remains the same.
    @Test
    public void test4() {
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        Sink sink = new SinkBuilder().build();
        // add chunks
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        // verify that the Sequenceanalyser contains two elements
        assertThat(sa.size(), is(2));
        // verify that chunks are dependent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
        // verify that the Sequenceanalyser still contains two elements
        assertThat(sa.size(), is(2));
        // remove chunk1
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that the Sequenceanalyser now contains one element
        assertThat(sa.size(), is(1));
        // verify that chunk2 is now present
        assertChunks(sa.getInactiveIndependentChunks(), chunk2);
        // verify that the Sequenceanalyser still contains one element
        assertThat(sa.size(), is(1));
    }

    private void assertChunks(List<ChunkIdentifier> inactiveIndependentChunks, Chunk... chunks) {
        assertThat("size of chunks arrays", inactiveIndependentChunks.size(), is(chunks.length));
        for (int i = 0; i < inactiveIndependentChunks.size(); i++) {
            assertThat("assert matching jobId", inactiveIndependentChunks.get(i).jobId, is(chunks[i].getJobId()));
            assertThat("assert matching chunkId", inactiveIndependentChunks.get(i).chunkId, is(chunks[i].getChunkId()));
        }
    }

    private Chunk createChunk(long jobId, long chunkId, String... keys) {
        return new ChunkBuilder().setJobId(jobId).setChunkId(chunkId).setKeys(new HashSet<String>(Arrays.asList(keys))).build();
    }
}
