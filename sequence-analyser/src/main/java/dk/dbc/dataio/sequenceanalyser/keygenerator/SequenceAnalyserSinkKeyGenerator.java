package dk.dbc.dataio.sequenceanalyser.keygenerator;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple sequence analyser key generator ensuring sequential ordering for all
 * chunks going to the same destination
 */
public class SequenceAnalyserSinkKeyGenerator implements SequenceAnalyserKeyGenerator {
    private final long sinkId;

    /**
     * @param sinkId the ID of the sink for which a key should be generated
     */
    public SequenceAnalyserSinkKeyGenerator(long sinkId) {
        this.sinkId = sinkId;
    }

    @Override
    public Set<String> generateKeys(List<String> data) {
        final HashSet<String> keys = new HashSet<>(1);
        keys.add(Long.toString(sinkId));
        return keys;
    }
}
