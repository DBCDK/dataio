package dk.dbc.dataio.commons.utils.test.model;

import dk.dbc.dataio.commons.types.SupplementaryProcessData;

public class SupplementaryProcessDataBuilder {

    private long submitter = 987654L;
    private String format = "format";

    public SupplementaryProcessDataBuilder setSubmitter(long submitter) {
        this.submitter = submitter;
        return this;
    }

    public SupplementaryProcessDataBuilder setFormat(String format) {
        this.format = format;
        return this;
    }

    public SupplementaryProcessData build() {
        return new SupplementaryProcessData(submitter, format);
    }

}
