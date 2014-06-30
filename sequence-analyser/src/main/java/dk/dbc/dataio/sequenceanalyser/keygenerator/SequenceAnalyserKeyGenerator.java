package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Chunk;
import dk.dbc.dataio.commons.types.Sink;

import java.util.Set;

/**
 * Common interface for sequence analyser key generators.
 */
public interface SequenceAnalyserKeyGenerator {
    /**
     * Generates keys determining chunk ordering during sequence analysis
     * @param chunk Chunk for which keys are generated
     * @param sink Chunk destination
     * @return set of keys
     */
    Set<String> generateKeys(Chunk chunk, Sink sink);
}
