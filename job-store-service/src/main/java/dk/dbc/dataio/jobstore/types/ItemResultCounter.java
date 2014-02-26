package dk.dbc.dataio.jobstore.types;

import java.io.Serializable;

public class ItemResultCounter implements Serializable {
    private static final long serialVersionUID = 4162100278280972759L;

    // counts
    private long success = 0;
    private long failure = 0;
    private long ignore = 0;

    public long getTotal() {
        return success + failure + ignore;
    }

    public long getSuccess() {
        return success;
    }

    public void incrementSuccess() {
        success++;
    }

    public long getFailure() {
        return failure;
    }

    public void incrementFailure() {
        failure++;
    }

    public long getIgnore() {
        return ignore;
    }

    public void incrementIgnore() {
        ignore++;
    }
}
