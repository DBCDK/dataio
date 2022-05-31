package dk.dbc.dataio.sink.periodicjobs;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import dk.dbc.dataio.commons.conversion.ConversionParam;

import java.util.Optional;

public class PeriodicJobsConversionParam extends ConversionParam {
    @JsonProperty
    private String sortkey;

    @JsonProperty
    private String recordHeader;

    @JsonProperty
    private String groupHeader;

    @JsonIgnore
    public Optional<String> getSortkey() {
        return Optional.ofNullable(sortkey);
    }

    public PeriodicJobsConversionParam withSortkey(String sortkey) {
        this.sortkey = sortkey;
        return this;
    }

    @JsonIgnore
    public Optional<String> getRecordHeader() {
        return Optional.ofNullable(recordHeader);
    }

    public PeriodicJobsConversionParam withRecordHeader(String recordHeader) {
        this.recordHeader = recordHeader;
        return this;
    }

    @JsonIgnore
    public Optional<String> getGroupHeader() {
        return Optional.ofNullable(groupHeader);
    }

    public PeriodicJobsConversionParam withGroupHeader(String groupHeader) {
        this.groupHeader = groupHeader;
        return this;
    }
}
