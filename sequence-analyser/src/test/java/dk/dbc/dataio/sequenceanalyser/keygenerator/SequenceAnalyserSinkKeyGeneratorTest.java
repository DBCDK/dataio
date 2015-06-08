package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElementIdentifier;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserSinkKeyGeneratorTest {
    private final SequenceAnalyserKeyGenerator keyGenerator = new SequenceAnalyserSinkKeyGenerator();
    private final CollisionDetectionElement element = new CollisionDetectionElement(
            new CollisionDetectionElementIdentifier() {}, new HashSet<String>());
    private final Sink sink = new SinkBuilder().build();

    @Test(expected = NullPointerException.class)
    public void generateKeys_sinkArgIsNull_throws() {
        keyGenerator.generateKeys(element, null);
    }

    @Test
    public void generateKeys_sinkArgIsValid_returnsKey() {
        final Sink sink = new SinkBuilder().build();
        final Set<String> keys = keyGenerator.generateKeys(null, sink);
        assertThat(keys, is(notNullValue()));
        assertThat(keys.size(), is(1));
        assertThat(keys.contains(sink.getContent().getName()), is(true));
    }

    @Test(expected = NullPointerException.class)
    public void constructor_sinkArgIsNull_throws() {
        new SequenceAnalyserSinkKeyGenerator(null);
    }

    @Test
    public void generateKeys() {
        final SequenceAnalyserSinkKeyGenerator generator = new SequenceAnalyserSinkKeyGenerator(sink);
        final Set<String> keys = generator.generateKeys(Arrays.asList("data1", "data2"));
        assertThat(keys, is(notNullValue()));
        assertThat(keys.size(), is(1));
        assertThat(keys.contains(sink.getContent().getName()), is(true));
    }
}