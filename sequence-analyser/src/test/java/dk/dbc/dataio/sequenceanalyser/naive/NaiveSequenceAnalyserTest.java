package dk.dbc.dataio.sequenceanalyser.naive;

import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElementIdentifier;
import dk.dbc.dataio.sequenceanalyser.SequenceAnalyser;
import org.junit.Before;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

public class NaiveSequenceAnalyserTest {
    static {
        System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "trace");
    }

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
        CollisionDetectionElement element = createCollisionDetectionElement(1, 2);
        sa.add(element);
        // THEN:
        // verify that chunk is independent
        assertElements(sa.getInactiveIndependent(100), element);
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
        CollisionDetectionElement element = createCollisionDetectionElement(1, 2);
        sa.add(element);
        // WHEN:
        // remove chunk
        sa.deleteAndRelease(new NaiveIdentifier(1, 2));
        // verify that there are no independent chunks left
        assertElements(sa.getInactiveIndependent(100));
        // verify that the sequence analyser is empty
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
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 1);
        CollisionDetectionElement element2 = createCollisionDetectionElement(2, 1, "horse");
        CollisionDetectionElement element3 = createCollisionDetectionElement(3, 1, "goat");
        CollisionDetectionElement element4 = createCollisionDetectionElement(4, 1);
        // add chunk
        sa.add(element1);
        sa.add(element2);
        sa.add(element3);
        sa.add(element4);
        // THEN:
        // verify that chunks are independent and inactive
        assertElements(sa.getInactiveIndependent(100), element1, element2, element3, element4);
        // verify number of elements in Sequence analyser
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
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "horse");
        sa.add(element1);
        // verify that there is one chunk in the sequence analyser
        assertThat(sa.size(), is(1));
        // WHEN
        // add the second chunk
        CollisionDetectionElement element2 = createCollisionDetectionElement(3, 4, "animal", "goat");
        sa.add(element2);
        // verify that there are two chunks in the sequence analyser
        assertThat(sa.size(), is(2));
        // THEN:
        // verify that only the first chunk is independent
        assertElements(sa.getInactiveIndependent(100), element1);
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
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "horse");
        CollisionDetectionElement element2 = createCollisionDetectionElement(3, 4, "animal", "goat");
        sa.add(element1);
        sa.add(element2);
        // verify that only the first chunk is independent
        assertElements(sa.getInactiveIndependent(100), element1);
        // verify that the seqence analyser contains two chunks
        assertThat(sa.size(), is(2));
        // wHEN
        // remove chunk1
        sa.deleteAndRelease(new NaiveIdentifier(1, 2));
        // verify that the sequence analyser contains one chunk
        assertThat(sa.size(), is(1));
        // THEN
        // verify that chunk2 is now independent
        assertElements(sa.getInactiveIndependent(100), element2);
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
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "horse");
        CollisionDetectionElement element2 = createCollisionDetectionElement(3, 4, "animal", "goat");
        // add chunks
        sa.add(element1);
        sa.add(element2);
        // verify that the sequence analyser contains two elements
        assertThat(sa.size(), is(2));
        // WHEN
        // verify that chunks are dependent
        assertElements(sa.getInactiveIndependent(100), element1);
        // verify that the sequence analyser still contains two elements
        assertThat(sa.size(), is(2));
        // THEN
        // verify that chunk2 has not become independent
        assertElements(sa.getInactiveIndependent(100));
        // verify that the sequence analyser still contains one element
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
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "bird", "eagle");
        CollisionDetectionElement element2 = createCollisionDetectionElement(1, 3, "animal", "mammal", "goat");
        CollisionDetectionElement element3 = createCollisionDetectionElement(4, 5, "animal", "mammal", "horse");
        // add chunks
        sa.add(element1);
        sa.add(element2);
        sa.add(element3);
        // verify that the sequence analyser contains three chunks
        assertThat(sa.size(), is(3));
        // verify that chunks are dependent
        assertElements(sa.getInactiveIndependent(100), element1);
        // remove chunk1
        sa.deleteAndRelease(new NaiveIdentifier(1, 2));
        // verify that the sequence analyser now contains two chunks
        assertThat(sa.size(), is(2));
        // verify that chunk2 is now present
        assertElements(sa.getInactiveIndependent(100), element2);
        // remove chunk2
        sa.deleteAndRelease(new NaiveIdentifier(1, 3));
        // verify that the sequence analyser now contains one chunk
        assertThat(sa.size(), is(1));
        // verify that chunk3 is now present
        assertElements(sa.getInactiveIndependent(100), element3);
    }

    /*
     * Given a sequence analyser with three independent chunks
     * When retrieving independent with a max limit of two
     * Then only the first two chunks are returned
     */
    @Test
    public void testOfMaxLimitForIndependentChunksRetrieval() {
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 1, "one");
        CollisionDetectionElement element2 = createCollisionDetectionElement(1, 2, "two");
        CollisionDetectionElement element3 = createCollisionDetectionElement(1, 3, "three");
        // add chunks
        sa.add(element1);
        sa.add(element2);
        sa.add(element3);
        // verify that chunks are dependent
        assertElements(sa.getInactiveIndependent(2), element1, element2);
    }

    @Test
    public void isHead_emptyDependencyGraph_returnFalse() {
        final CollisionDetectionElementIdentifier cid = new NaiveIdentifier(7, 9);
        assertThat(sa.isHead(cid), is(false));
    }

    @Test
    public void isHead_onElementInDependencyGraph_returnTrue() {
        final CollisionDetectionElementIdentifier cid = new NaiveIdentifier(7, 9);
        CollisionDetectionElement element = createCollisionDetectionElement(7, 9, "a");
        sa.add(element);
        assertThat(sa.isHead(cid), is(true));
    }

    @Test
    public void isHead_secondElementInDependencyGraph_returnFalse() {
        CollisionDetectionElement element1 = createCollisionDetectionElement(7, 9, "a");
        CollisionDetectionElement element2 = createCollisionDetectionElement(11, 13, "b");
        sa.add(element1);
        sa.add(element2);

        final CollisionDetectionElementIdentifier cid = new NaiveIdentifier(11, 13);
        assertThat(sa.isHead(cid), is(false));
    }

    @Test
    public void isHead_IndependentChunksHandled_isHeadReturnsCorrectValues() {
        CollisionDetectionElement element1 = createCollisionDetectionElement(7, 9, "a");
        CollisionDetectionElement element2 = createCollisionDetectionElement(11, 13, "b");
        sa.add(element1);
        sa.add(element2);

        final CollisionDetectionElementIdentifier cid = new NaiveIdentifier(11, 13);

        assertThat(sa.isHead(cid), is(false));
        assertElements(sa.getInactiveIndependent(100), element1, element2);
        assertThat(sa.isHead(cid), is(false));
        sa.deleteAndRelease(new NaiveIdentifier(7, 9));
        assertThat(sa.isHead(cid), is(true));
        sa.deleteAndRelease(cid);
        assertThat(sa.isHead(cid), is(false));
    }

    @Test
    public void isHead_dependentChunksHandled_isHeadReturnsCorrectValues() {
        CollisionDetectionElement element1 = createCollisionDetectionElement(7, 9, "a");
        CollisionDetectionElement element2 = createCollisionDetectionElement(11, 13, "a");
        sa.add(element1);
        sa.add(element2);

        final CollisionDetectionElementIdentifier cid = new NaiveIdentifier(11, 13);

        assertThat(sa.isHead(cid), is(false));
        assertElements(sa.getInactiveIndependent(100), element1);
        assertThat(sa.isHead(cid), is(false));
        sa.deleteAndRelease(new NaiveIdentifier(7, 9));
        assertThat(sa.isHead(cid), is(true));
        assertElements(sa.getInactiveIndependent(100), element2);
        assertThat(sa.isHead(cid), is(true));
        sa.deleteAndRelease(cid);
        assertThat(sa.isHead(cid), is(false));
    }
    
    private CollisionDetectionElement createCollisionDetectionElement(int primary, int secondary, String... keys) {
        return new CollisionDetectionElement(new NaiveIdentifier(primary, secondary), new HashSet<>(Arrays.asList(keys)));
    }

    private void assertElements(List<CollisionDetectionElement> inactiveIndependentChunks, CollisionDetectionElement... elements) {
        assertThat("size of chunks arrays", inactiveIndependentChunks.size(), is(elements.length));
        for (int i = 0; i < inactiveIndependentChunks.size(); i++) {
            assertThat("assert matching identifier", inactiveIndependentChunks.get(i).getIdentifier(),
                    is(elements[i].getIdentifier()));
        }
    }

    private static class NaiveIdentifier implements CollisionDetectionElementIdentifier {
        private final int primary;
        private final int secondary;

        public NaiveIdentifier(int primary, int secondary) {
            this.primary = primary;
            this.secondary = secondary;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            NaiveIdentifier that = (NaiveIdentifier) o;

            if (primary != that.primary) {
                return false;
            }
            return secondary == that.secondary;

        }

        @Override
        public int hashCode() {
            int result = primary;
            result = 31 * result + secondary;
            return result;
        }
    }
}
