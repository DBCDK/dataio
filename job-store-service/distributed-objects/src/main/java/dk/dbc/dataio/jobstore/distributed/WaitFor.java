package dk.dbc.dataio.jobstore.distributed;

import java.io.Serializable;
import java.util.Comparator;

public record WaitFor(int sinkId, int submitter, String matchKey) implements Serializable, Comparable<WaitFor> {
    private static final Comparator<WaitFor> COMPARATOR = Comparator.comparing(WaitFor::sinkId).thenComparing(WaitFor::submitter).thenComparing(WaitFor::matchKey);

    @Override
    public int compareTo(WaitFor o) {
        return COMPARATOR.compare(this, o);
    }
}
