package dk.dbc.dataio.sequenceanalyser.keygenerator;

import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * Simple sequence analyser key generator ensuring no ordering for chunks
 */
public class SequenceAnalyserNoOrderKeyGenerator implements SequenceAnalyserKeyGenerator {
    @Override
    public Set<String> generateKeys(List<String> data) {
        return Collections.emptySet();
    }
}
