package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.test.model.ChunkBuilder;
import dk.dbc.dataio.commons.utils.test.model.SinkBuilder;
import org.junit.Test;

import java.util.Set;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.junit.Assert.assertThat;

public class SequenceAnalyserSinkKeyGeneratorTest {
    private final SequenceAnalyserKeyGenerator keyGenerator = new SequenceAnalyserSinkKeyGenerator();
    private final Chunk chunk = new ChunkBuilder().build();

    @Test(expected = NullPointerException.class)
    public void generateKeys_sinkArgIsNull_throws() {
        keyGenerator.generateKeys(chunk, null);
    }

    @Test
    public void generateKeys_sinkArgIsValid_returnsKey() {
        final Sink sink = new SinkBuilder().build();
        final Set<String> keys = keyGenerator.generateKeys(null, sink);
        assertThat(keys, is(notNullValue()));
        assertThat(keys.size(), is(1));
        assertThat(keys.contains(sink.getContent().getName()), is(true));
    }
}