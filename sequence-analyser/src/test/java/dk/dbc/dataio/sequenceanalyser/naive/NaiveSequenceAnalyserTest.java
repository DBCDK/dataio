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
        sa = new NaiveSequenceAnalyser();
    }

    /*
     * Given: An empty Sequence Analyser
     * When : A single chunk is inserted
     * Then : The chunk must be independent when retrieved.
     */
    @Test
    public void insertionAndRetrievalOfSingleChunk() {
        // GIVEN:
        // Sequence analyser must be empty
        assertThat(sa.size(), is(0));
        // WHEN:
        Chunk chunk = createChunk(1L, 2L);
        Sink sink = new SinkBuilder().build();
        sa.addChunk(chunk, sink);
        // THEN:
        // verify that chunk is independent
        assertChunks(sa.getInactiveIndependentChunks(), chunk);
    }

    /*
     * Given: A Sequence Analyser with a single chunk
     * When : The chunk is released and deleted
     * Then : No more independent chunks must exist in the sequence analyser,
     *        and the sequence analyser must be empty.
     */
    @Test
    public void deleteAndReleaseOfSingleChunk() {
        // GIVEN:
        Chunk chunk = createChunk(1L, 2L);
        Sink sink = new SinkBuilder().build();
        sa.addChunk(chunk, sink);
        // WHEN:
        // remove chunk
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that there are no independent chunks left
        assertChunks(sa.getInactiveIndependentChunks());
        // verify that the sequenceanalyser is empty
        assertThat(sa.size(), is(0));
    }

    /*
     * Given: An empty sequence.
     * Given: A sequence analyser with two independent chunks destined to the same sink.
     * When : Two independent chunks with same sink are inserted
     * Then : Both chunks must be retreived as inacative and independent.
     */
    @Test
    public void testInsertionAndRetrivalOfTwoIndependentChunks() {
        // GIVEN:
        assertThat(sa.size(), is(0));
        // WHEN:
        Chunk chunk1 = createChunk(1L, 2L, "horse");
        Chunk chunk2 = createChunk(3L, 4L, "goat");
        Sink sink = new SinkBuilder().build();
        // add chunk
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        // THEN:
        // verify that chunks are independent and inactive
        assertChunks(sa.getInactiveIndependentChunks(), chunk1, chunk2);
        // verify number of elements in Sequenceanalyser
        assertThat(sa.size(), is(2));
    }

    /*
     * Given: A sequence analyser with a single chunk
     * When : A new chunk for the same sink, which depends on the existing chunk, is inserted,
     * Then : Only the first chunk can be retrived
     */
    @Test
    public void insertDependentChunkWhichDependsOnExistingChunk() {
        // GIVEN:
        // add first chunk
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Sink sink = new SinkBuilder().build();
        sa.addChunk(chunk1, sink);
        // verify that there is one chunk in the sequence analyser
        assertThat(sa.size(), is(1));
        // WHEN
        // add the second chunk
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        sa.addChunk(chunk2, sink);
        // verify that tehere are two chunks in the sequence analyser
        assertThat(sa.size(), is(2));
        // THEN:
        // verify that only the first chunk is independent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
    }

    /*
     * Given: A seqeuce analyser with two chunks where one depends on the other.
     * When : The first chunk is released
     * Then : The second chunk can be retrieved.
     */
    @Test
    public void deletionAndReleaseOfChunkMakesDependendChunkIndependent() {
        // GIVEN:
        // add chunkS
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        Sink sink = new SinkBuilder().build();
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        // verify that only the first chunk is independent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
        // verify that the seqence analyser contains two chunks
        assertThat(sa.size(), is(2));
        // wHEN
        // remove chunk1
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that the seqence analyser contains one chunk
        assertThat(sa.size(), is(1));
        // THEN
        // verify that chunk2 is now independent
        assertChunks(sa.getInactiveIndependentChunks(), chunk2);
    }

    // Test: Two chunks with overlapping keys, but destined for different sinks.
    //       They should not depend on each other.
    /*
     * Given: An empty Seqeuence analyser
     * When : Two chunk with overlapping keys, but destined for different sinks are inserted
     * Then : Both chunks must be independent.
     */
    @Test
    public void twoChunksWithOverlappingKeysDestinedDifferentSinksMustBeIndependent() {
        // GIVEN
        // verify that sequence analyser is empty
        assertThat(sa.size(), is(0));
        // WHEN:
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        Sink sink1 = new SinkBuilder().setId(1L).build();
        Sink sink2 = new SinkBuilder().setId(2L).build();
        // add chunks
        sa.addChunk(chunk1, sink1);
        sa.addChunk(chunk2, sink2);
        // THEN
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

    /*
     * Given: A sequence analyser with two dependent chunks
     * When : a ChunkIdentifier is retrieved with getInactiveIndependentChunksAndActivate(),
     * Then : subsequent calls to getInactiveIndependentChunksAndActivate() will not return the same ChunkIdentifer,
     *        but the size of the internal dependecy graph remains the same.
     */
    @Test
    public void alreadyRetreivedChunkMustNotReappearAsIndependent() {
        // GIVEN
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        Sink sink = new SinkBuilder().build();
        // add chunks
        sa.addChunk(chunk1, sink);
        sa.addChunk(chunk2, sink);
        // verify that the Sequenceanalyser contains two elements
        assertThat(sa.size(), is(2));
        // WHEN
        // verify that chunks are dependent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
        // verify that the Sequenceanalyser still contains two elements
        assertThat(sa.size(), is(2));
        // THEN
        // verify that chunk2 has not become independent
        assertChunks(sa.getInactiveIndependentChunks());
        // verify that the Sequenceanalyser still contains one element
        assertThat(sa.size(), is(2));
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
