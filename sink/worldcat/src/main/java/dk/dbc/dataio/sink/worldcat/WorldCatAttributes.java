package dk.dbc.dataio.sink.worldcat;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public class WorldCatAttributes {
    private String pid;
    private String ocn;
    private List<Holding> holdings;

    @JsonProperty
    private boolean lhr;

    public String getPid() {
        return pid;
    }

    public WorldCatAttributes withPid(String pid) {
        this.pid = pid;
        return this;
    }

    public String getOcn() {
        return ocn;
    }

    public WorldCatAttributes withOcn(String ocn) {
        this.ocn = ocn;
        return this;
    }

    public List<Holding> getHoldings() {
        return holdings;
    }

    public WorldCatAttributes withHoldings(List<Holding> holdings) {
        this.holdings = holdings;
        return this;
    }

    public Boolean hasLhr() {
        return lhr;
    }

    public WorldCatAttributes withLhr(boolean lhr) {
        this.lhr = lhr;
        return this;
    }

    @Override
    public String toString() {
        return "WorldCatAttributes{" +
                "pid='" + pid + '\'' +
                ", ocn='" + ocn + '\'' +
                ", lhr=" + lhr +
                ", holdings=" + holdings +
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

        WorldCatAttributes that = (WorldCatAttributes) o;

        if (lhr != that.lhr) {
            return false;
        }
        if (pid != null ? !pid.equals(that.pid) : that.pid != null) {
            return false;
        }
        if (ocn != null ? !ocn.equals(that.ocn) : that.ocn != null) {
            return false;
        }
        return holdings != null ? holdings.equals(that.holdings) : that.holdings == null;
    }

    @Override
    public int hashCode() {
        int result = pid != null ? pid.hashCode() : 0;
        result = 31 * result + (ocn != null ? ocn.hashCode() : 0);
        result = 31 * result + (lhr ? 1 : 0);
        result = 31 * result + (holdings != null ? holdings.hashCode() : 0);
        return result;
    }
}
