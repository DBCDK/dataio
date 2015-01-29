package dk.dbc.dataio.commons.types;

import java.io.Serializable;


public class SupplementaryProcessData  implements Serializable {

    private /* final */ long submitter;
    private /* final */ String format;

    private SupplementaryProcessData() {}

    public SupplementaryProcessData(long submitter, String format) {
        this.submitter = submitter;
        this.format = format;
    }

    public long getSubmitter() {
        return submitter;
    }

    public String getFormat() {
        return format;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SupplementaryProcessData)) return false;

        SupplementaryProcessData that = (SupplementaryProcessData) o;

        if (submitter != that.submitter) return false;
        if (format != null ? !format.equals(that.format) : that.format != null) return false;

        return true;
    }

    @Override
    public int hashCode() {
        int result = (int) (submitter ^ (submitter >>> 32));
        result = 31 * result + (format != null ? format.hashCode() : 0);
        return result;
    }
}
