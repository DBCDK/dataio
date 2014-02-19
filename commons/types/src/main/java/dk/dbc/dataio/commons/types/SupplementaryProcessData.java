package dk.dbc.dataio.commons.types;


public class SupplementaryProcessData {

    private /* final */ String submitter;
    private /* final */ String format;

    public SupplementaryProcessData(String submitter, String format) {
        this.submitter = submitter;
        this.format = format;
    }

    public String getSubmitter() {
        return submitter;
    }

    public String getFormat() {
        return format;
    }
}
