package dk.dbc.dataio.harvester;

import java.time.Instant;

/**
 * Time interval representation consisting of a from and a to point in time
 */
public class TimeInterval {
    private final Instant from;
    private final Instant to;

    public TimeInterval(Instant from, Instant to) {
        this.from = from;
        this.to = to;
    }

    public Instant getFrom() {
        return from;
    }

    public Instant getTo() {
        return to;
    }

    @Override
    public String toString() {
        return "TimeInterval{" +
                "from=" + from +
                ", to=" + to +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }

        TimeInterval that = (TimeInterval) o;

        if (from != null ? !from.equals(that.from) : that.from != null) {
            return false;
        }
        return to != null ? to.equals(that.to) : that.to == null;
    }

    @Override
    public int hashCode() {
        int result = from != null ? from.hashCode() : 0;
        result = 31 * result + (to != null ? to.hashCode() : 0);
        return result;
    }
}
