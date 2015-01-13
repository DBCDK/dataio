package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import dk.dbc.dataio.sequenceanalyser.ChunkIdentifier;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;
import org.junit.Test;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserNoOrderKeyGeneratorTest {
    private final SequenceAnalyserKeyGenerator keyGenerator = new SequenceAnalyserNoOrderKeyGenerator();
    private final CollisionDetectionElement element = new CollisionDetectionElement(new ChunkIdentifier(5L, 1L), new HashSet<String>());

    @Test
    public void generateKeys_sinkArgIsNull_returnsEmptyKeySet() {
        final Set<String> keys = keyGenerator.generateKeys(element, null);
        assertThat(keys, is(notNullValue()));
        assertThat(keys.isEmpty(), is(true));
    }

    @Test
    public void generateKeys_sinkArgIsNotNull_returnsEmptyKeySet() {
        final Sink sink = new SinkBuilder().build();
        final Set<String> keys = keyGenerator.generateKeys(null, sink);
        assertThat(keys, is(notNullValue()));
        assertThat(keys.size(), is(0));
    }

    @Test
    public void generateKeys_returnsEmptyKeySet() {
        final Set<String> keys = keyGenerator.generateKeys(Arrays.asList("data1", "data2"));
        assertThat(keys, is(notNullValue()));
        assertThat(keys.size(), is(0));
    }
}