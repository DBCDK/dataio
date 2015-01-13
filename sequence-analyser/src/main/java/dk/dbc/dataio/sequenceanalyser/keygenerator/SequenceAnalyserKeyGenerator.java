package dk.dbc.dataio.sequenceanalyser.keygenerator;

import dk.dbc.dataio.commons.types.Sink;
import dk.dbc.dataio.sequenceanalyser.CollisionDetectionElement;

import java.util.List;
import java.util.Set;

/**
 * Common interface for sequence analyser key generators.
 */
public interface SequenceAnalyserKeyGenerator {
    /** DEPRECATED
     * Generates keys determining chunk ordering during sequence analysis
     * @param chunk Chunk for which keys are generated
     * @param sink Chunk destination
     * @return set of keys
     */
    Set<String> generateKeys(CollisionDetectionElement element, Sink sink);

    /**
     * Generates keys determining chunk ordering during sequence analysis
     * @param data chunk data for which keys are generated
     * @return set of keys
     */
    Set<String> generateKeys(List<String> data);
}
