package dk.dbc.dataio.engine;

public class Job {
    private final long id;

    public Job(long id) {
        this.id = id;
    }

    public long getId() {
        return id;
    }

    public FlowInfo getFlowInfo() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
