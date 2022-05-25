package dk.dbc.dataio.jobstore.types;

import org.junit.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.Assert.fail;

public class SequenceAnalysisDataTest {
    @Test(expected = NullPointerException.class)
    public void constructor_dataArgIsNull_throws() {
        new SequenceAnalysisData(null);
    }

    @Test
    public void constructor_dataArgIsNonEmpty_returnsNewInstance() {
        final Set<String> expectedData = new HashSet(Arrays.asList("first", "second"));
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        assertThat(sequenceAnalysisData, is(notNullValue()));
        assertThat(sequenceAnalysisData.getData(), is(expectedData));
    }

    @Test
    public void constructor_dataArgIsEmpty_returnsNewInstance() {
        final Set<String> expectedData = Collections.emptySet();
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        assertThat(sequenceAnalysisData, is(notNullValue()));
        assertThat(sequenceAnalysisData.getData(), is(expectedData));
    }

    @Test
    public void getData_returnedListIsUnmodifiable() {
        final Set<String> expectedData = new HashSet(Arrays.asList("first", "second"));
        final SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        try {
            sequenceAnalysisData.getData().add("third");
            fail("No exception thrown");
        } catch (UnsupportedOperationException e) {}
    }
}
