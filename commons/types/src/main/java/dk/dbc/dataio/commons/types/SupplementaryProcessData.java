package dk.dbc.dataio.commons.types;


public class SupplementaryProcessData {

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
}
