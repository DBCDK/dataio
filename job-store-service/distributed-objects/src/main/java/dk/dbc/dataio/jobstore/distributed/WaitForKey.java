package dk.dbc.dataio.jobstore.distributed;

import java.io.Serializable;
import java.util.Comparator;

public record WaitForKey(int sinkId, int submitter, String matchKey) implements Serializable, Comparable<WaitForKey> {
    private static final Comparator<WaitForKey> COMPARATOR = Comparator.comparing(WaitForKey::sinkId).thenComparing(WaitForKey::submitter).thenComparing(WaitForKey::matchKey);

    @Override
    public int compareTo(WaitForKey o) {
        return COMPARATOR.compare(this, o);
    }
}
