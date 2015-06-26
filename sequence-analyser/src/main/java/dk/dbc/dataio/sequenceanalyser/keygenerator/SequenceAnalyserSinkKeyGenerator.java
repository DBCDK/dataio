package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.commons.utils.invariant.InvariantUtil;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple sequence analyser key generator ensuring sequential ordering for all
 * chunks going to the same destination
 */
public class SequenceAnalyserSinkKeyGenerator implements SequenceAnalyserKeyGenerator {
    private final Sink sink;

    public SequenceAnalyserSinkKeyGenerator() {
        sink = null;
    }

    /**
     * @param sink the sink for which a key should be generated
     * @throws NullPointerException if given null-valued sink
     */
    public SequenceAnalyserSinkKeyGenerator(Sink sink) throws NullPointerException {
        this.sink = InvariantUtil.checkNotNullOrThrow(sink, "sink");
    }

    /**
     * @throws NullPointerException if given null-valued sink
     */
    @Override
    public Set<String> generateKeys(CollisionDetectionElement element, Sink sink) throws NullPointerException {
        InvariantUtil.checkNotNullOrThrow(sink, "sink");
        final HashSet<String> keys = new HashSet<>(1);
        keys.add(sink.getContent().getName());
        return keys;
    }

    @Override
    public Set<String> generateKeys(List<String> data) {
        final HashSet<String> keys = new HashSet<>(1);
        keys.add(sink.getContent().getName());
        return keys;
    }
}
