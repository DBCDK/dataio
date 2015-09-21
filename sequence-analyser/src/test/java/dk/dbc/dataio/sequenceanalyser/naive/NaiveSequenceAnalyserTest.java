/*
 * DataIO - Data IO
 * Copyright (C) 2015 Dansk Bibliotekscenter a/s, Tempovej 7-11, DK-2750 Ballerup,
 * Denmark. CVR: 15149043
 *
 * This file is part of DataIO.
 *
 * DataIO is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * DataIO is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with DataIO.  If not, see <http://www.gnu.org/licenses/>.
 */

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
     * When : A single element is inserted
     * Then : The element must be independent when retrieved.
     */
    @Test
    public void insertionAndRetrievalOfSingleElement() {
        // GIVEN:
        // Sequence analyser must be empty
        assertThat(sa.size(), is(0));
        // WHEN:
        CollisionDetectionElement element = createCollisionDetectionElement(1, 2);
        sa.add(element);
        // THEN:
        // verify that element is independent
        assertElements(sa.getInactiveIndependent(100), element);
    }

    /*
     * Given: A Sequence Analyser with a single element
     * When : The element is released and deleted
     * Then : No more independent elements must exist in the sequence analyser,
     *        and the sequence analyser must be empty.
     */
    @Test
    public void deleteAndReleaseOfSingleElement() {
        // GIVEN:
        CollisionDetectionElement element = createCollisionDetectionElement(1, 2);
        sa.add(element);
        // WHEN:
        // remove element
        sa.deleteAndRelease(new NaiveIdentifier(1, 2));
        // verify that there are no independent elements left
        assertElements(sa.getInactiveIndependent(100));
        // verify that the sequence analyser is empty
        assertThat(sa.size(), is(0));
    }

    /*
     * Given: An empty sequence.
     * Given: A sequence analyser with four independent elements.
     * When : Four independent elements are inserted
     * Then : All elements must be retrieved as inactive and independent.
     */
    @Test
    public void testInsertionAndRetrievalOfTwoIndependentElements() {
        // GIVEN:
        assertThat(sa.size(), is(0));
        // WHEN:
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 1);
        CollisionDetectionElement element2 = createCollisionDetectionElement(2, 1, "horse");
        CollisionDetectionElement element3 = createCollisionDetectionElement(3, 1, "goat");
        CollisionDetectionElement element4 = createCollisionDetectionElement(4, 1);
        // add element
        sa.add(element1);
        sa.add(element2);
        sa.add(element3);
        sa.add(element4);
        // THEN:
        // verify that elements are independent and inactive
        assertElements(sa.getInactiveIndependent(100), element1, element2, element3, element4);
        // verify number of elements in Sequence analyser
        assertThat(sa.size(), is(4));
    }

    /*
     * Given: A sequence analyser with a single element
     * When : A new element which depends on the existing element, is inserted,
     * Then : Only the first element can be retrieved
     */
    @Test
    public void insertDependentElementWhichDependsOnExistingElement() {
        // GIVEN:
        // add first element
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "horse");
        sa.add(element1);
        // verify that there is one element in the sequence analyser
        assertThat(sa.size(), is(1));
        // WHEN
        // add the second element
        CollisionDetectionElement element2 = createCollisionDetectionElement(3, 4, "animal", "goat");
        sa.add(element2);
        // verify that there are two elements in the sequence analyser
        assertThat(sa.size(), is(2));
        // THEN:
        // verify that only the first element is independent
        assertElements(sa.getInactiveIndependent(100), element1);
    }

    /*
     * Given: A seqeuce analyser with two elements where one depends on the other.
     * When : The first element is released
     * Then : The second element can be retrieved.
     */
    @Test
    public void deletionAndReleaseOfElementMakesDependendElementIndependent() {
        // GIVEN:
        // add elementS
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "horse");
        CollisionDetectionElement element2 = createCollisionDetectionElement(3, 4, "animal", "goat");
        sa.add(element1);
        sa.add(element2);
        // verify that only the first element is independent
        assertElements(sa.getInactiveIndependent(100), element1);
        // verify that the seqence analyser contains two elements
        assertThat(sa.size(), is(2));
        // wHEN
        // remove element1
        sa.deleteAndRelease(new NaiveIdentifier(1, 2));
        // verify that the sequence analyser contains one element
        assertThat(sa.size(), is(1));
        // THEN
        // verify that element2 is now independent
        assertElements(sa.getInactiveIndependent(100), element2);
    }

    /*
     * Given: A sequence analyser with two dependent elements
     * When : a ElementIdentifier is retrieved with getInactiveIndependentElementsAndActivate(),
     * Then : subsequent calls to getInactiveIndependentElementsAndActivate() will not return the same ElementIdentifier,
     *        but the size of the internal dependency graph remains the same.
     */
    @Test
    public void alreadyRetrievedElementMustNotReappearAsIndependent() {
        // GIVEN
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "horse");
        CollisionDetectionElement element2 = createCollisionDetectionElement(3, 4, "animal", "goat");
        // add elements
        sa.add(element1);
        sa.add(element2);
        // verify that the sequence analyser contains two elements
        assertThat(sa.size(), is(2));
        // WHEN
        // verify that elements are dependent
        assertElements(sa.getInactiveIndependent(100), element1);
        // verify that the sequence analyser still contains two elements
        assertThat(sa.size(), is(2));
        // THEN
        // verify that element2 has not become independent
        assertElements(sa.getInactiveIndependent(100));
        // verify that the sequence analyser still contains one element
        assertThat(sa.size(), is(2));
    }

    /*
     * A more intervowen test:
     * Given a sequence analyser with three interdependent elements,
     * i.e. element2 depends on element1, and element3 depends on element1 and element2.
     * When element1 is released, only element2 must be independent.
     * When element2 is released, element3 must be independent.
     */
    @Test
    public void testOfDependencyBetweenThreeElements() {
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 2, "animal", "bird", "eagle");
        CollisionDetectionElement element2 = createCollisionDetectionElement(1, 3, "animal", "mammal", "goat");
        CollisionDetectionElement element3 = createCollisionDetectionElement(4, 5, "animal", "mammal", "horse");
        // add elements
        sa.add(element1);
        sa.add(element2);
        sa.add(element3);
        // verify that the sequence analyser contains three elements
        assertThat(sa.size(), is(3));
        // verify that elements are dependent
        assertElements(sa.getInactiveIndependent(100), element1);
        // remove element1
        sa.deleteAndRelease(new NaiveIdentifier(1, 2));
        // verify that the sequence analyser now contains two elements
        assertThat(sa.size(), is(2));
        // verify that element2 is now present
        assertElements(sa.getInactiveIndependent(100), element2);
        // remove element2
        sa.deleteAndRelease(new NaiveIdentifier(1, 3));
        // verify that the sequence analyser now contains one element
        assertThat(sa.size(), is(1));
        // verify that element3 is now present
        assertElements(sa.getInactiveIndependent(100), element3);
    }

    /*
     * Given a sequence analyser with three independent elements consuming one slot each
     * When retrieving independent with a max limit of two
     * Then only the first two elements are returned
     */
    @Test
    public void testOfMaxLimitForIndependentElementsRetrieval() {
        CollisionDetectionElement element1 = createCollisionDetectionElement(1, 1, "one");
        CollisionDetectionElement element2 = createCollisionDetectionElement(1, 2, "two");
        CollisionDetectionElement element3 = createCollisionDetectionElement(1, 3, "three");
        // add elements
        sa.add(element1);
        sa.add(element2);
        sa.add(element3);
        // verify that elements are dependent
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
    public void isHead_IndependentElementsHandled_isHeadReturnsCorrectValues() {
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
    public void isHead_dependentElementsHandled_isHeadReturnsCorrectValues() {
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
        return new CollisionDetectionElement(new NaiveIdentifier(primary, secondary), new HashSet<>(Arrays.asList(keys)), 1);
    }

    private void assertElements(List<CollisionDetectionElement> inactiveIndependentElements, CollisionDetectionElement... elements) {
        assertThat("size of elements arrays", inactiveIndependentElements.size(), is(elements.length));
        for (int i = 0; i < inactiveIndependentElements.size(); i++) {
            assertThat("assert matching identifier", inactiveIndependentElements.get(i).getIdentifier(),
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
