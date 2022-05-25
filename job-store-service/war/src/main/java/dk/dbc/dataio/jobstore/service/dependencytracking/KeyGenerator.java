package dk.dbc.dataio.jobstore.service.dependencytracking;

import java.util.List;
import java.util.Set;

/**
 * Common interface for dependency tracking key generators.
 */
public interface KeyGenerator {
    /**
     * Generates keys determining chunk ordering during sequence analysis
     * @param data chunk data for which keys are generated
     * @return set of keys
     */
    Set<String> getKeys(List<String> data);
}
