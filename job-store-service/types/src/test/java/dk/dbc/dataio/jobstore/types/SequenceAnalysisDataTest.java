package dk.dbc.dataio.jobstore.types;

import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class SequenceAnalysisDataTest {
    @Test
    public void constructor_dataArgIsNull_throws() {
        assertThrows(NullPointerException.class, () -> new SequenceAnalysisData(null));
    }

    @Test
    public void constructor_dataArgIsNonEmpty_returnsNewInstance() {
        Set<String> expectedData = Set.of("first", "second");
        SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        assertThat(sequenceAnalysisData, is(notNullValue()));
        assertThat(sequenceAnalysisData.getData(), is(expectedData));
    }

    @Test
    public void constructor_dataArgIsEmpty_returnsNewInstance() {
        Set<String> expectedData = Set.of();
        SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        assertThat(sequenceAnalysisData, is(notNullValue()));
        assertThat(sequenceAnalysisData.getData(), is(expectedData));
    }

    @Test
    public void getData_returnedListIsUnmodifiable() {
        Set<String> expectedData = Set.of("first", "second");
        SequenceAnalysisData sequenceAnalysisData = new SequenceAnalysisData(expectedData);
        assertThrows(UnsupportedOperationException.class, () -> sequenceAnalysisData.getData().add("third"));
    }
}
