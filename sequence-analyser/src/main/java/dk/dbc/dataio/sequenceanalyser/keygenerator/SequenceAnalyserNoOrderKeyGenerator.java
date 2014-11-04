package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;

import java.util.Collections;
import java.util.Set;

/**
 * Simple sequence analyser key generator ensuring no ordering for chunks
 */
public class SequenceAnalyserNoOrderKeyGenerator implements SequenceAnalyserKeyGenerator {
    @Override
    public Set<String> generateKeys(Chunk chunk, Sink sink) {
        return Collections.emptySet();
    }
}
