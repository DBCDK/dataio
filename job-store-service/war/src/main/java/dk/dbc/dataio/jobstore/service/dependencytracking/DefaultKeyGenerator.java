package dk.dbc.dataio.jobstore.service.dependencytracking;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Simple dependency tracking key generator ensuring duplicates
 * are removed from given list.
 */
public class DefaultKeyGenerator implements KeyGenerator {
    @Override
    public Set<String> getKeys(List<String> tokens) {
        if (tokens != null) {
            return new HashSet<>(tokens);
        }
        return Collections.emptySet();
    }
}
