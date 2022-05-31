package dk.dbc.dataio.jobstore.types;

import dk.dbc.dataio.commons.types.SinkContent;
import dk.dbc.invariant.InvariantUtil;

import java.io.Serializable;

public class SinkStatusSnapshot implements Serializable {
    private static final long serialVersionUID = -4446705379560875610L;

    private long sinkId;
    private SinkContent.SinkType type;
    private String name;
    private int numberOfJobs;
    private int numberOfChunks;

    public long getSinkId() {
        return sinkId;
    }

    public SinkStatusSnapshot withSinkId(long sinkId) {
        this.sinkId = sinkId;
        return this;
    }

    public SinkContent.SinkType getType() {
        return type;
    }

    public SinkStatusSnapshot withSinkType(SinkContent.SinkType type) {
        this.type = type;
        return this;
    }

    public String getName() {
        return name;
    }

    public SinkStatusSnapshot withName(String name) {
        this.name = InvariantUtil.checkNotNullNotEmptyOrThrow(name, "name");
        return this;
    }

    public int getNumberOfJobs() {
        return numberOfJobs;
    }

    public SinkStatusSnapshot withNumberOfJobs(int numberOfJobs) {
        this.numberOfJobs = numberOfJobs;
        return this;
    }

    public int getNumberOfChunks() {
        return numberOfChunks;
    }

    public SinkStatusSnapshot withNumberOfChunks(int numberOfChunks) {
        this.numberOfChunks = numberOfChunks;
        return this;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SinkStatusSnapshot)) return false;

        SinkStatusSnapshot that = (SinkStatusSnapshot) o;

        if (numberOfJobs != that.numberOfJobs) return false;
        if (numberOfChunks != that.numberOfChunks) return false;
        if (type != that.type) return false;
        return name != null ? name.equals(that.name) : that.name == null;
    }

    @Override
    public int hashCode() {
        int result = type != null ? type.hashCode() : 0;
        result = 31 * result + (name != null ? name.hashCode() : 0);
        result = 31 * result + numberOfJobs;
        result = 31 * result + numberOfChunks;
        return result;
    }
}
