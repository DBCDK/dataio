package dk.dbc.dataio.sequenceanalyser.keygenerator;

import java.util.List;
import java.util.Set;

/**
 * Common interface for sequence analyser key generators.
 */
public interface SequenceAnalyserKeyGenerator {
    /**
     * Generates keys determining chunk ordering during sequence analysis
     * @param data chunk data for which keys are generated
     * @return set of keys
     */
    Set<String> generateKeys(List<String> data);
}
