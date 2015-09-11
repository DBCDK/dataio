package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import org.junit.Test;

import java.util.Arrays;
import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserSinkKeyGeneratorTest {
    private final Sink sink = new SinkBuilder().build();

    @Test
    public void generateKeys() {
        final SequenceAnalyserSinkKeyGenerator generator = new SequenceAnalyserSinkKeyGenerator(sink.getId());
        final Set<String> keys = generator.generateKeys(Arrays.asList("data1", "data2"));
        assertThat(keys, is(notNullValue()));
        assertThat(keys.size(), is(1));
        assertThat(keys.contains(Long.toString(sink.getId())), is(true));
    }
}