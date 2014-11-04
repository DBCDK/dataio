package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.ext.XLogger;
import org.slf4j.ext.XLoggerFactory;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

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
        sa.addChunk(chunk);
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
        sa.addChunk(chunk);
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
     * Given: A sequence analyser with four independent chunks.
     * When : Four independent chunks are inserted
     * Then : All chunks must be retrieved as inactive and independent.
     */
    @Test
    public void testInsertionAndRetrievalOfTwoIndependentChunks() {
        // GIVEN:
        assertThat(sa.size(), is(0));
        // WHEN:
        Chunk chunk1 = createChunk(1L, 1L);
        Chunk chunk2 = createChunk(2L, 1L, "horse");
        Chunk chunk3 = createChunk(3L, 1L, "goat");
        Chunk chunk4 = createChunk(4L, 1L);
        // add chunk
        sa.addChunk(chunk1);
        sa.addChunk(chunk2);
        sa.addChunk(chunk3);
        sa.addChunk(chunk4);
        // THEN:
        // verify that chunks are independent and inactive
        assertChunks(sa.getInactiveIndependentChunks(), chunk1, chunk2, chunk3, chunk4);
        // verify number of elements in Sequenceanalyser
        assertThat(sa.size(), is(4));
    }

    /*
     * Given: A sequence analyser with a single chunk
     * When : A new chunk which depends on the existing chunk, is inserted,
     * Then : Only the first chunk can be retrieved
     */
    @Test
    public void insertDependentChunkWhichDependsOnExistingChunk() {
        // GIVEN:
        // add first chunk
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        sa.addChunk(chunk1);
        // verify that there is one chunk in the sequence analyser
        assertThat(sa.size(), is(1));
        // WHEN
        // add the second chunk
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        sa.addChunk(chunk2);
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
        sa.addChunk(chunk1);
        sa.addChunk(chunk2);
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

    /*
     * Given: A sequence analyser with two dependent chunks
     * When : a ChunkIdentifier is retrieved with getInactiveIndependentChunksAndActivate(),
     * Then : subsequent calls to getInactiveIndependentChunksAndActivate() will not return the same ChunkIdentifier,
     *        but the size of the internal dependency graph remains the same.
     */
    @Test
    public void alreadyRetrievedChunkMustNotReappearAsIndependent() {
        // GIVEN
        Chunk chunk1 = createChunk(1L, 2L, "animal", "horse");
        Chunk chunk2 = createChunk(3L, 4L, "animal", "goat");
        // add chunks
        sa.addChunk(chunk1);
        sa.addChunk(chunk2);
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

    /*
     * A more intervowen test:
     * Given a sequence analyser with three interdependent chunks,
     * i.e. chunk2 depends on chunk1, and chunk3 depends on chunk1 and chunk2.
     * When chunk1 is released, only chunk2 must be independent.
     * When chunk2 is released, chunk3 must be independent.
     */
    @Test
    public void testOfDependencyBetweenThreeChunks() {
        Chunk chunk1 = createChunk(1L, 2L, "animal", "bird", "eagle");
        Chunk chunk2 = createChunk(1L, 3L, "animal", "mammal", "goat");
        Chunk chunk3 = createChunk(4L, 5L, "animal", "mammal", "horse");
        // add chunks
        sa.addChunk(chunk1);
        sa.addChunk(chunk2);
        sa.addChunk(chunk3);
        // verify that the sequence analyser contains three chunks
        assertThat(sa.size(), is(3));
        // verify that chunks are dependent
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
        // remove chunk1
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 2L));
        // verify that the sequence analyser now contains two chunks
        assertThat(sa.size(), is(2));
        // verify that chunk2 is now present
        assertChunks(sa.getInactiveIndependentChunks(), chunk2);
        // remove chunk2
        sa.deleteAndReleaseChunk(new ChunkIdentifier(1L, 3L));
        // verify that the sequence analyser now contains one chunk
        assertThat(sa.size(), is(1));
        // verify that chunk3 is now present
        assertChunks(sa.getInactiveIndependentChunks(), chunk3);
    }

    @Test
    public void isHead_emptyDependencyGraph_returnFalse() {
        ChunkIdentifier cid = new ChunkIdentifier(7L, 9L);
        assertThat(sa.isHead(cid), is(false));
    }

    @Test
    public void isHead_onElementInDependencyGraph_returnTrue() {
        ChunkIdentifier cid = new ChunkIdentifier(7L, 9L);
        Chunk chunk = createChunk(7L, 9L, "a");
        sa.addChunk(chunk);
        assertThat(sa.isHead(cid), is(true));
    }

    @Test
    public void isHead_secondElementInDependencyGraph_returnFalse() {
        Chunk chunk1 = createChunk(7L, 9L, "a");
        Chunk chunk2 = createChunk(11L, 13L, "b");
        sa.addChunk(chunk1);
        sa.addChunk(chunk2);

        ChunkIdentifier cid = new ChunkIdentifier(11L, 13L);
        assertThat(sa.isHead(cid), is(false));
    }

    @Test
    public void isHead_IndependentChunksHandled_isHeadReturnsCorrectValues() {
        Chunk chunk1 = createChunk(7L, 9L, "a");
        Chunk chunk2 = createChunk(11L, 13L, "b");
        sa.addChunk(chunk1);
        sa.addChunk(chunk2);

        ChunkIdentifier cid = new ChunkIdentifier(11L, 13L);

        assertThat(sa.isHead(cid), is(false));
        assertChunks(sa.getInactiveIndependentChunks(), chunk1, chunk2);
        assertThat(sa.isHead(cid), is(false));
        sa.deleteAndReleaseChunk(new ChunkIdentifier(7L, 9L));
        assertThat(sa.isHead(cid), is(true));
        sa.deleteAndReleaseChunk(cid);
        assertThat(sa.isHead(cid), is(false));
    }

    @Test
    public void isHead_dependentChunksHandled_isHeadReturnsCorrectValues() {
        Chunk chunk1 = createChunk(7L, 9L, "a");
        Chunk chunk2 = createChunk(11L, 13L, "a");
        sa.addChunk(chunk1);
        sa.addChunk(chunk2);

        ChunkIdentifier cid = new ChunkIdentifier(11L, 13L);

        assertThat(sa.isHead(cid), is(false));
        assertChunks(sa.getInactiveIndependentChunks(), chunk1);
        assertThat(sa.isHead(cid), is(false));
        sa.deleteAndReleaseChunk(new ChunkIdentifier(7L, 9L));
        assertThat(sa.isHead(cid), is(true));
        assertChunks(sa.getInactiveIndependentChunks(), chunk2);
        assertThat(sa.isHead(cid), is(true));
        sa.deleteAndReleaseChunk(cid);
        assertThat(sa.isHead(cid), is(false));
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
