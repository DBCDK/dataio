package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;

import java.util.HashSet;
import java.util.Set;

/**
 * Simple sequence analyser key generator ensuring sequential ordering for all
 * chunks going to the same destination
 */
public class SequenceAnalyserSinkKeyGenerator implements SequenceAnalyserKeyGenerator {
    /**
     * @throws NullPointerException if given null-valued sink
     */
    @Override
    public Set<String> generateKeys(Chunk chunk, Sink sink) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(sink, "sink");
        final HashSet<String> keys = new HashSet<>(1);
        keys.add(sink.getContent().getName());
        return keys;
    }
}
